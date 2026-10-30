package com.linjian.seckill.domain.service;

import com.linjian.seckill.domain.entity.SkActivityGoods;
import com.linjian.seckill.domain.entity.SkGoods;
import com.linjian.seckill.domain.entity.SkIdempotentRecord;
import com.linjian.seckill.domain.entity.SkSeckillOrder;
import com.linjian.seckill.domain.mapper.SkActivityGoodsMapper;
import com.linjian.seckill.domain.mapper.SkGoodsMapper;
import com.linjian.seckill.domain.mapper.SkIdempotentRecordMapper;
import com.linjian.seckill.domain.mapper.SkSeckillOrderMapper;
import com.linjian.seckill.domain.mq.SeckillOrderMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 异步落单的幂等与补偿测试。
 * <p>
 * 这几条是整个「预扣即扣」方案里最容易写错的地方：写错了不会报错，
 * 只会表现为「库存越来越少」或「用户抢到但没有订单」，而且极难复现。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long ACTIVITY_ID = 1001L;
    private static final Long GOODS_ID = 2001L;
    private static final Long AG_ID = 4001L;
    private static final Long USER_ID = 3001L;
    private static final Long REQUEST_ID = 1234567890123L;

    @Mock
    private SkSeckillOrderMapper orderMapper;

    @Mock
    private SkActivityGoodsMapper activityGoodsMapper;

    @Mock
    private SkGoodsMapper goodsMapper;

    @Mock
    private SkIdempotentRecordMapper idempotentMapper;

    @Mock
    private SeckillResultCache resultCache;

    @Mock
    private StockRedisService stockRedisService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "timeoutMinutes", 15);
        // 让 TransactionTemplate 直接同步执行回调，行为接近真实但不需要 Spring 容器
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        lenient().doAnswer(invocation -> {
            SkIdempotentRecord record = invocation.getArgument(0);
            record.setId(1L);
            return 1;
        }).when(idempotentMapper).insert(any());
    }

    @Test
    @DisplayName("重复消息：幂等表插入冲突，直接跳过且不落单")
    void shouldSkipDuplicatedMessage() {
        doThrow(new DuplicateKeyException("uk_message")).when(idempotentMapper).insert(any());

        orderService.handleMessage(message());

        verify(orderMapper, never()).insert(any());
        verify(resultCache, never()).markSuccess(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("DB 库存不足：回补 Redis 并标记失败，不重试")
    void shouldCompensateWhenDbStockEmpty() {
        SkActivityGoods relation = new SkActivityGoods();
        relation.setId(AG_ID);
        relation.setGoodsId(GOODS_ID);
        relation.setStock(0);
        relation.setVersion(0);
        relation.setSeckillPrice(new BigDecimal("39.90"));
        when(activityGoodsMapper.selectById(AG_ID)).thenReturn(relation);
        // 条件更新返回 0 就代表库存不足，不需要「先查再判断」的竞态窗口
        when(activityGoodsMapper.deductStock(eq(AG_ID), anyInt())).thenReturn(0);

        orderService.handleMessage(message());

        verify(stockRedisService).restore(ACTIVITY_ID, GOODS_ID, USER_ID, 1);
        verify(orderMapper, never()).insert(any());
        // 幂等记录必须保留：删掉的话消息重投会再补偿一次，Redis 库存被反复加回去
        verify(idempotentMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("唯一索引冲突（同一用户已买过）：按重复购买补偿")
    void shouldCompensateWhenDuplicateBuy() {
        SkActivityGoods relation = new SkActivityGoods();
        relation.setId(AG_ID);
        relation.setGoodsId(GOODS_ID);
        relation.setStock(10);
        relation.setVersion(0);
        relation.setSeckillPrice(new BigDecimal("39.90"));
        when(activityGoodsMapper.selectById(AG_ID)).thenReturn(relation);
        when(activityGoodsMapper.deductStock(eq(AG_ID), anyInt())).thenReturn(1);

        SkGoods goods = new SkGoods();
        goods.setId(GOODS_ID);
        goods.setName("测试商品");
        when(goodsMapper.selectById(GOODS_ID)).thenReturn(goods);
        when(orderMapper.insert(any())).thenThrow(new DuplicateKeyException("uk_user_activity_goods"));

        orderService.handleMessage(message());

        verify(stockRedisService).restore(ACTIVITY_ID, GOODS_ID, USER_ID, 1);
        verify(idempotentMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("落单成功：事务提交后才写结果，订单号与 requestId 都正确")
    void shouldMarkSuccessAfterCommit() {
        SkActivityGoods relation = new SkActivityGoods();
        relation.setId(AG_ID);
        relation.setGoodsId(GOODS_ID);
        relation.setStock(10);
        relation.setVersion(0);
        relation.setSeckillPrice(new BigDecimal("39.90"));
        when(activityGoodsMapper.selectById(AG_ID)).thenReturn(relation);
        when(activityGoodsMapper.deductStock(eq(AG_ID), anyInt())).thenReturn(1);

        SkGoods goods = new SkGoods();
        goods.setId(GOODS_ID);
        goods.setName("测试商品");
        when(goodsMapper.selectById(GOODS_ID)).thenReturn(goods);

        orderService.handleMessage(message());

        verify(orderMapper).insert(any(SkSeckillOrder.class));
        verify(resultCache).markSuccess(eq(REQUEST_ID), any(), anyString());
        verify(stockRedisService, never()).restore(anyLong(), anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("系统异常：回滚幂等记录后抛出，让 MQ 重投")
    void shouldRollbackIdempotentOnSystemError() {
        when(activityGoodsMapper.selectById(AG_ID)).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> orderService.handleMessage(message()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db down");

        // 删掉幂等记录，重投时才能重新处理
        verify(idempotentMapper).deleteById(1L);
        verify(stockRedisService, never()).restore(anyLong(), anyLong(), anyLong(), anyInt());
    }

    private SeckillOrderMessage message() {
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setMessageId("msg-1");
        message.setRequestId(REQUEST_ID);
        message.setUserId(USER_ID);
        message.setActivityId(ACTIVITY_ID);
        message.setActivityGoodsId(AG_ID);
        message.setGoodsId(GOODS_ID);
        message.setQuantity(1);
        return message;
    }
}
