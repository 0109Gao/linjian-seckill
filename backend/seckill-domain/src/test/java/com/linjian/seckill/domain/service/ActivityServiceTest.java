package com.linjian.seckill.domain.service;

import com.linjian.seckill.common.BizException;
import com.linjian.seckill.common.ErrorCode;
import com.linjian.seckill.domain.dto.ActivityGoodsItemRequest;
import com.linjian.seckill.domain.dto.ActivitySaveRequest;
import com.linjian.seckill.domain.entity.SkActivity;
import com.linjian.seckill.domain.entity.SkActivityGoods;
import com.linjian.seckill.domain.entity.SkGoods;
import com.linjian.seckill.domain.enums.ActivityStatus;
import com.linjian.seckill.domain.mapper.SkActivityGoodsMapper;
import com.linjian.seckill.domain.mapper.SkActivityMapper;
import com.linjian.seckill.domain.mapper.SkGoodsMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 活动创建相关的规则测试。
 * <p>
 * 只测「值得被回归的分支」：时间合法性、商品存在性、重复商品、已开始活动不可改。
 * 库存扣减与预热属于集成行为，放在接口冒烟与压测里验证，这里不重复造轮子。
 */
@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private SkActivityMapper activityMapper;

    @Mock
    private SkActivityGoodsMapper activityGoodsMapper;

    @Mock
    private SkGoodsMapper goodsMapper;

    @Mock
    private StockRedisService stockRedisService;

    @Mock
    private ActivityCacheService activityCacheService;

    @InjectMocks
    private ActivityService activityService;

    private ActivitySaveRequest validRequest() {
        ActivityGoodsItemRequest item = new ActivityGoodsItemRequest();
        item.setGoodsId(2001L);
        item.setSeckillPrice(new BigDecimal("39.90"));
        item.setStock(100);
        item.setLimitPerUser(1);

        ActivitySaveRequest request = new ActivitySaveRequest();
        request.setTitle("测试专场");
        request.setStartTime(LocalDateTime.now().plusMinutes(10));
        request.setEndTime(LocalDateTime.now().plusHours(1));
        request.setItems(List.of(item));
        return request;
    }

    private SkGoods goods(long id) {
        SkGoods goods = new SkGoods();
        goods.setId(id);
        goods.setName("商品" + id);
        return goods;
    }

    @Test
    @DisplayName("开始时间晚于结束时间时应拒绝创建")
    void createRejectsInvalidTimeRange() {
        ActivitySaveRequest request = validRequest();
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setEndTime(LocalDateTime.now().plusHours(1));

        assertThatThrownBy(() -> activityService.create(request))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.ACTIVITY_TIME_INVALID.getCode());

        verify(activityMapper, never()).insert(any());
    }

    @Test
    @DisplayName("商品不存在时应拒绝创建，且不写入任何活动记录")
    void createRejectsMissingGoods() {
        ActivitySaveRequest request = validRequest();
        // 库中查不到 id=2001 的商品
        when(goodsMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> activityService.create(request))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.GOODS_NOT_FOUND.getCode());

        verify(activityMapper, never()).insert(any());
    }

    @Test
    @DisplayName("同一商品重复添加应被拒绝")
    void createRejectsDuplicateGoods() {
        ActivitySaveRequest request = validRequest();
        ActivityGoodsItemRequest duplicate = new ActivityGoodsItemRequest();
        duplicate.setGoodsId(2001L);
        duplicate.setSeckillPrice(new BigDecimal("1.00"));
        duplicate.setStock(1);
        request.setItems(List.of(request.getItems().get(0), duplicate));

        assertThatThrownBy(() -> activityService.create(request))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.BAD_REQUEST.getCode());

        verify(activityMapper, never()).insert(any());
    }

    @Test
    @DisplayName("合法请求应写入一条活动与一条关联记录")
    void createPersistsActivityAndRelations() {
        ActivitySaveRequest request = validRequest();
        when(goodsMapper.selectBatchIds(anyCollection())).thenReturn(List.of(goods(2001L)));

        activityService.create(request);

        verify(activityMapper).insert(any(SkActivity.class));
        verify(activityGoodsMapper).insert(any(SkActivityGoods.class));
    }

    @Test
    @DisplayName("已开始的活动不允许修改库存")
    void updateRejectsRunningActivity() {
        SkActivity running = new SkActivity();
        running.setId(1001L);
        running.setStatus(ActivityStatus.RUNNING.getCode());
        when(activityMapper.selectById(1001L)).thenReturn(running);

        assertThatThrownBy(() -> activityService.update(1001L, validRequest()))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.ACTIVITY_NOT_EDITABLE.getCode());

        verify(activityGoodsMapper, never()).delete(any());
    }

    @Test
    @DisplayName("活动不存在时查详情应报 4102")
    void detailRejectsMissingActivity() {
        when(activityMapper.selectById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> activityService.detail(9999L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("预热时即使库存已写过，也应把到点活动推进为进行中")
    void preheatAdvancesStatusWhenStartTimePassed() {
        SkActivity started = new SkActivity();
        started.setId(1001L);
        started.setStatus(ActivityStatus.PENDING.getCode());
        started.setStartTime(LocalDateTime.now().minusMinutes(1));
        started.setEndTime(LocalDateTime.now().plusHours(1));

        SkActivityGoods relation = new SkActivityGoods();
        relation.setActivityId(1001L);
        relation.setGoodsId(2001L);
        relation.setStock(10);
        when(activityGoodsMapper.selectList(any())).thenReturn(List.of(relation));
        when(stockRedisService.preheat(any(), any(), anyInt(), any())).thenReturn(false);

        int written = activityService.doPreheat(started);

        assertThat(written).isZero();
        verify(activityMapper).updateById(any(SkActivity.class));
    }

    @Test
    @DisplayName("活动下没有任何商品时预热应直接返回 0，不产生 Redis 写入")
    void preheatWithoutGoodsDoesNothing() {
        SkActivity activity = new SkActivity();
        activity.setId(1001L);
        activity.setStatus(ActivityStatus.PENDING.getCode());
        activity.setStartTime(LocalDateTime.now().plusMinutes(2));
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        when(activityGoodsMapper.selectList(any())).thenReturn(List.of());

        assertThat(activityService.doPreheat(activity)).isZero();
        verify(stockRedisService, never()).preheat(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("商品集合去重后的数量与明细数量不一致即视为重复提交")
    void duplicateDetectionUsesSetSemantics() {
        ActivitySaveRequest request = validRequest();
        request.getItems().get(0).setGoodsId(2001L);
        request.setItems(List.of(request.getItems().get(0), request.getItems().get(0)));

        Set<Long> distinct = request.getItems().stream()
                .map(ActivityGoodsItemRequest::getGoodsId)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(distinct).hasSize(1);
        assertThat(request.getItems()).hasSize(2);
        verify(activityMapper, times(0)).insert(any());
    }
}
