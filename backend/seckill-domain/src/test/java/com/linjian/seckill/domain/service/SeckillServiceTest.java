package com.linjian.seckill.domain.service;

import com.linjian.seckill.common.BizException;
import com.linjian.seckill.common.ErrorCode;
import com.linjian.seckill.domain.config.StockRedisTemplateHolder;
import com.linjian.seckill.domain.dto.ActivityGoodsMeta;
import com.linjian.seckill.domain.dto.SeckillResult;
import com.linjian.seckill.domain.entity.SkSeckillOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 秒杀入口的分支测试。
 * <p>
 * 重点不在「正常路径能返回 PENDING」，而在几条容易被写错的边界：
 * 限购命中后要不要回查订单、预扣失败要不要标记结果、消息落库失败要不要回补。
 * 这三条错了不会报错，只会在线上表现为「用户白抢」或「库存凭空消失」。
 */
@ExtendWith(MockitoExtension.class)
class SeckillServiceTest {

    private static final Long ACTIVITY_ID = 1001L;
    private static final Long GOODS_ID = 2001L;
    private static final Long USER_ID = 3001L;

    @Mock
    private ActivityCacheService activityCacheService;

    @Mock
    private StockRedisService stockRedisService;

    @Mock
    private SeckillResultCache resultCache;

    @Mock
    private LocalMessageService localMessageService;

    @Mock
    private OrderService orderService;

    @Mock
    private StockRedisTemplateHolder redisHolder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SeckillService seckillService;

    @BeforeEach
    void setUp() {
        lenient().when(redisHolder.template()).thenReturn(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
    }

    @Test
    @DisplayName("未预热：返回库存未预热错误，不调用 Lua 扣减")
    void shouldRejectWhenNotPreheated() {
        when(activityCacheService.get(ACTIVITY_ID, GOODS_ID)).thenReturn(null);

        assertThatThrownBy(() -> seckillService.seckill(ACTIVITY_ID, GOODS_ID, USER_ID))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.STOCK_NOT_PREHEATED.getCode());

        verify(stockRedisService, never()).deduct(anyLong(), anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("活动未开始：拒绝且不消耗库存")
    void shouldRejectBeforeStart() {
        when(activityCacheService.get(ACTIVITY_ID, GOODS_ID)).thenReturn(meta(false));

        assertThatThrownBy(() -> seckillService.seckill(ACTIVITY_ID, GOODS_ID, USER_ID))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.ACTIVITY_CLOSED.getCode());

        verify(stockRedisService, never()).deduct(anyLong(), anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("频控命中：同一用户 1 秒内第二次请求被拒")
    void shouldRejectByRateLimit() {
        when(activityCacheService.get(ACTIVITY_ID, GOODS_ID)).thenReturn(meta(true));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> seckillService.seckill(ACTIVITY_ID, GOODS_ID, USER_ID))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS.getCode());
    }

    @Test
    @DisplayName("库存不足：标记结果失败，避免前端轮询一直 PENDING")
    void shouldMarkFailedWhenStockEmpty() {
        when(activityCacheService.get(ACTIVITY_ID, GOODS_ID)).thenReturn(meta(true));
        when(stockRedisService.deduct(ACTIVITY_ID, GOODS_ID, USER_ID, 1, 1))
                .thenReturn(StockRedisService.INSUFFICIENT);

        assertThatThrownBy(() -> seckillService.seckill(ACTIVITY_ID, GOODS_ID, USER_ID))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.STOCK_EMPTY.getCode());

        verify(resultCache).markFailed(anyLong(), eq(ErrorCode.STOCK_EMPTY.getCode()), anyString());
        verify(localMessageService, never()).saveAndSend(any());
    }

    @Test
    @DisplayName("超出限购但已有订单：直接返回成功，不让用户以为白抢")
    void shouldReturnExistingOrderWhenLimitExceeded() {
        when(activityCacheService.get(ACTIVITY_ID, GOODS_ID)).thenReturn(meta(true));
        when(stockRedisService.deduct(ACTIVITY_ID, GOODS_ID, USER_ID, 1, 1))
                .thenReturn(StockRedisService.LIMIT_EXCEEDED);
        SkSeckillOrder existing = new SkSeckillOrder();
        existing.setId(9L);
        existing.setOrderNo("SK20261030999");
        when(orderService.findByUserAndGoods(ACTIVITY_ID, GOODS_ID, USER_ID)).thenReturn(existing);

        SeckillResult result = seckillService.seckill(ACTIVITY_ID, GOODS_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_SUCCESS);
        assertThat(result.getOrderNo()).isEqualTo("SK20261030999");
    }

    @Test
    @DisplayName("超出限购且无订单：返回重复购买错误")
    void shouldRejectDuplicateBuy() {
        when(activityCacheService.get(ACTIVITY_ID, GOODS_ID)).thenReturn(meta(true));
        when(stockRedisService.deduct(ACTIVITY_ID, GOODS_ID, USER_ID, 1, 1))
                .thenReturn(StockRedisService.LIMIT_EXCEEDED);
        when(orderService.findByUserAndGoods(ACTIVITY_ID, GOODS_ID, USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> seckillService.seckill(ACTIVITY_ID, GOODS_ID, USER_ID))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.DUPLICATE_BUY.getCode());
    }

    @Test
    @DisplayName("预扣成功：落本地消息并返回 PENDING")
    void shouldReturnPendingAfterDeduct() {
        when(activityCacheService.get(ACTIVITY_ID, GOODS_ID)).thenReturn(meta(true));
        when(stockRedisService.deduct(ACTIVITY_ID, GOODS_ID, USER_ID, 1, 1)).thenReturn(99L);

        SeckillResult result = seckillService.seckill(ACTIVITY_ID, GOODS_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_PENDING);
        assertThat(result.getRequestId()).isNotNull();
        verify(resultCache).markPending(result.getRequestId());
        verify(localMessageService).saveAndSend(any());
    }

    @Test
    @DisplayName("消息落库失败：必须回补 Redis，否则库存永久悬空")
    void shouldRestoreRedisWhenMessageSaveFails() {
        when(activityCacheService.get(ACTIVITY_ID, GOODS_ID)).thenReturn(meta(true));
        when(stockRedisService.deduct(ACTIVITY_ID, GOODS_ID, USER_ID, 1, 1)).thenReturn(99L);
        org.mockito.Mockito.doThrow(new IllegalStateException("db down"))
                .when(localMessageService).saveAndSend(any());

        assertThatThrownBy(() -> seckillService.seckill(ACTIVITY_ID, GOODS_ID, USER_ID))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());

        verify(stockRedisService).restore(ACTIVITY_ID, GOODS_ID, USER_ID, 1);
    }

    private ActivityGoodsMeta meta(boolean started) {
        ActivityGoodsMeta meta = new ActivityGoodsMeta();
        meta.setActivityId(ACTIVITY_ID);
        meta.setGoodsId(GOODS_ID);
        meta.setActivityGoodsId(4001L);
        meta.setGoodsName("测试商品");
        meta.setPrice("39.90");
        meta.setLimitPerUser(1);
        long now = System.currentTimeMillis();
        meta.setStartTime(started ? now - 60_000 : now + 60_000);
        meta.setEndTime(now + 3_600_000);
        return meta;
    }
}
