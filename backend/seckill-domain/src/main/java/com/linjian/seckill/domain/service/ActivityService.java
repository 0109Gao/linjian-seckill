package com.linjian.seckill.domain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linjian.seckill.common.BizException;
import com.linjian.seckill.common.ErrorCode;
import com.linjian.seckill.common.PageResult;
import com.linjian.seckill.domain.dto.ActivityDetailVO;
import com.linjian.seckill.domain.dto.ActivityGoodsItemRequest;
import com.linjian.seckill.domain.dto.ActivityGoodsVO;
import com.linjian.seckill.domain.dto.ActivitySaveRequest;
import com.linjian.seckill.domain.dto.ActivityVO;
import com.linjian.seckill.domain.entity.SkActivity;
import com.linjian.seckill.domain.entity.SkActivityGoods;
import com.linjian.seckill.domain.entity.SkGoods;
import com.linjian.seckill.domain.enums.ActivityStatus;
import com.linjian.seckill.domain.mapper.SkActivityGoodsMapper;
import com.linjian.seckill.domain.mapper.SkActivityMapper;
import com.linjian.seckill.domain.mapper.SkGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 活动服务：创建、查询、预热、结束清理。
 * <p>
 * 活动与商品是「一活动多商品」关系，因此每次创建活动都要写一批
 * sk_activity_goods 行，库存也落在这些行上。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    /** 库存键在活动结束后的保留时长：留一段时间用于关单回补与人工对账 */
    private static final Duration STOCK_TTL_AFTER_END = Duration.ofHours(1);

    private final SkActivityMapper activityMapper;
    private final SkActivityGoodsMapper activityGoodsMapper;
    private final SkGoodsMapper goodsMapper;
    private final StockRedisService stockRedisService;
    private final ActivityCacheService activityCacheService;

    // ------------------------------------------------------------
    // 写操作（仅后台管理端）
    // ------------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public Long create(ActivitySaveRequest request) {
        validateTime(request.getStartTime(), request.getEndTime());

        Set<Long> goodsIds = request.getItems().stream()
                .map(ActivityGoodsItemRequest::getGoodsId)
                .collect(Collectors.toSet());
        if (goodsIds.size() != request.getItems().size()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "同一商品不能重复添加到活动");
        }
        assertGoodsExist(goodsIds);

        SkActivity activity = new SkActivity();
        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setStatus(ActivityStatus.PENDING.getCode());
        activityMapper.insert(activity);

        for (ActivityGoodsItemRequest item : request.getItems()) {
            SkActivityGoods relation = new SkActivityGoods();
            relation.setActivityId(activity.getId());
            relation.setGoodsId(item.getGoodsId());
            relation.setSeckillPrice(item.getSeckillPrice());
            relation.setStock(item.getStock());
            relation.setLimitPerUser(item.getLimitPerUser() == null ? 1 : item.getLimitPerUser());
            activityGoodsMapper.insert(relation);
        }

        log.info("活动创建成功 activityId={} 商品数={} 开始时间={}",
                activity.getId(), request.getItems().size(), request.getStartTime());
        return activity.getId();
    }

    /**
     * 修改活动。仅「待开始」状态可改：已开始活动的库存可能已经扣过，
     * 改总额会让 Redis 与 DB 的直接对不上。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long activityId, ActivitySaveRequest request) {
        SkActivity activity = mustGet(activityId);
        if (!ActivityStatus.PENDING.getCode().equals(activity.getStatus())) {
            throw new BizException(ErrorCode.ACTIVITY_NOT_EDITABLE);
        }
        validateTime(request.getStartTime(), request.getEndTime());

        SkActivity update = new SkActivity();
        update.setId(activityId);
        update.setTitle(request.getTitle());
        update.setDescription(request.getDescription());
        update.setStartTime(request.getStartTime());
        update.setEndTime(request.getEndTime());
        activityMapper.updateById(update);

        // 商品明细整体替换：先删后插比逐条 diff 简单，且此时库存必为初始值，
        // 不存在丢扣减的风险
        activityGoodsMapper.delete(Wrappers.<SkActivityGoods>lambdaQuery()
                .eq(SkActivityGoods::getActivityId, activityId));
        for (ActivityGoodsItemRequest item : request.getItems()) {
            SkActivityGoods relation = new SkActivityGoods();
            relation.setActivityId(activityId);
            relation.setGoodsId(item.getGoodsId());
            relation.setSeckillPrice(item.getSeckillPrice());
            relation.setStock(item.getStock());
            relation.setLimitPerUser(item.getLimitPerUser() == null ? 1 : item.getLimitPerUser());
            activityGoodsMapper.insert(relation);
        }
        log.info("活动修改成功 activityId={} 商品数={}", activityId, request.getItems().size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long activityId) {
        SkActivity activity = mustGet(activityId);
        if (ActivityStatus.FINISHED.getCode().equals(activity.getStatus())) {
            throw new BizException(ErrorCode.ACTIVITY_NOT_EDITABLE, "活动已结束，无法取消");
        }
        SkActivity update = new SkActivity();
        update.setId(activityId);
        update.setStatus(ActivityStatus.CANCELLED.getCode());
        activityMapper.updateById(update);

        clearCache(activityId);
        log.info("活动已取消 activityId={}", activityId);
    }

    // ------------------------------------------------------------
    // 读操作
    // ------------------------------------------------------------

    public PageResult<ActivityVO> page(long pageNum, long pageSize, Integer status) {
        Page<SkActivity> page = new Page<>(pageNum, pageSize);
        IPage<SkActivity> result = activityMapper.selectPage(page,
                Wrappers.<SkActivity>lambdaQuery()
                        .eq(status != null, SkActivity::getStatus, status)
                        .orderByDesc(SkActivity::getStartTime));

        List<SkActivity> records = result.getRecords();
        Map<Long, Integer> countMap = countGoodsByActivity(
                records.stream().map(SkActivity::getId).toList());

        return PageResult.of(
                records.stream().map(a -> toVO(a, countMap.getOrDefault(a.getId(), 0))).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ActivityDetailVO detail(Long activityId) {
        SkActivity activity = mustGet(activityId);
        List<SkActivityGoods> relations = activityGoodsMapper.selectList(
                Wrappers.<SkActivityGoods>lambdaQuery()
                        .eq(SkActivityGoods::getActivityId, activityId));

        Map<Long, SkGoods> goodsMap = loadGoods(relations);

        List<ActivityGoodsVO> items = relations.stream().map(relation -> {
            SkGoods goods = goodsMap.get(relation.getGoodsId());
            ActivityGoodsVO vo = new ActivityGoodsVO();
            vo.setActivityGoodsId(relation.getId());
            vo.setGoodsId(relation.getGoodsId());
            if (goods != null) {
                vo.setName(goods.getName());
                vo.setSubtitle(goods.getSubtitle());
                vo.setImageUrl(goods.getImageUrl());
                vo.setOriginPrice(goods.getOriginPrice());
            }
            vo.setSeckillPrice(relation.getSeckillPrice());
            vo.setStock(relation.getStock());
            vo.setLimitPerUser(relation.getLimitPerUser());
            // 未预热时返回 null，前端据此展示「即将开始」而不是「已售罄」
            vo.setRemaining(stockRedisService.remaining(activityId, relation.getGoodsId()));
            return vo;
        }).toList();

        ActivityDetailVO vo = new ActivityDetailVO();
        vo.setId(activity.getId());
        vo.setTitle(activity.getTitle());
        vo.setDescription(activity.getDescription());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(activity.getStatus());
        vo.setStatusDesc(describe(activity.getStatus()));
        vo.setItems(items);
        return vo;
    }

    // ------------------------------------------------------------
    // 预热
    // ------------------------------------------------------------

    /**
     * 预热某个活动的全部商品库存到 Redis。
     * <p>
     * 幂等：SETNX 保证重复调用不会重置已扣减的库存。返回实际写入的商品数，
     * 0 表示此前已预热过。
     */
    public int preheat(Long activityId) {
        SkActivity activity = mustGet(activityId);
        if (ActivityStatus.CANCELLED.getCode().equals(activity.getStatus())) {
            throw new BizException(ErrorCode.ACTIVITY_CLOSED, "活动已取消，无需预热");
        }
        return doPreheat(activity);
    }

    /**
     * 真正的预热动作，供定时任务与手动接口共用。
     */
    public int doPreheat(SkActivity activity) {
        List<SkActivityGoods> relations = activityGoodsMapper.selectList(
                Wrappers.<SkActivityGoods>lambdaQuery()
                        .eq(SkActivityGoods::getActivityId, activity.getId()));
        if (relations.isEmpty()) {
            return 0;
        }

        Duration ttl = ttlOf(activity);
        Map<Long, SkGoods> goodsMap = loadGoods(relations);
        int written = 0;
        for (SkActivityGoods relation : relations) {
            if (stockRedisService.preheat(activity.getId(), relation.getGoodsId(), relation.getStock(), ttl)) {
                written++;
            }
            // 元信息与库存同 TTL、同一次预热写入：秒杀接口扣减前只读 Redis，
            // 不查活动表与商品表
            activityCacheService.warm(activity, relation, goodsMap.get(relation.getGoodsId()), ttl);
        }

        // 到点之后把状态推进为「进行中」，让查询接口能反映真实状态
        if (!ActivityStatus.RUNNING.getCode().equals(activity.getStatus())
                && !activity.getStartTime().isAfter(LocalDateTime.now())) {
            SkActivity update = new SkActivity();
            update.setId(activity.getId());
            update.setStatus(ActivityStatus.RUNNING.getCode());
            activityMapper.updateById(update);
        }

        if (written > 0) {
            log.info("活动预热完成 activityId={} 写入={}/{}", activity.getId(), written, relations.size());
        }
        return written;
    }

    /**
     * 结束到点的活动并清理库存键。
     */
    public void finish(SkActivity activity) {
        SkActivity update = new SkActivity();
        update.setId(activity.getId());
        update.setStatus(ActivityStatus.FINISHED.getCode());
        activityMapper.updateById(update);

        clearCache(activity.getId());
        log.info("活动已结束并清理库存 activityId={}", activity.getId());
    }

    /**
     * 清掉活动在 Redis 里的全部痕迹：库存、已购计数、元信息。
     * <p>
     * 元信息必须先于库存清理：如果先删库存键，这中间到来的秒杀请求会读到
     * 「元信息还在、库存没了」的半残状态，被误判成「未预热」而不是「已结束」。
     */
    private void clearCache(Long activityId) {
        List<SkActivityGoods> relations = activityGoodsMapper.selectList(
                Wrappers.<SkActivityGoods>lambdaQuery()
                        .eq(SkActivityGoods::getActivityId, activityId));
        relations.forEach(relation -> activityCacheService.evict(activityId, relation.getGoodsId()));
        stockRedisService.clearActivity(activityId);
    }

    // ------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------

    private void validateTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BizException(ErrorCode.ACTIVITY_TIME_INVALID, "开始时间必须早于结束时间");
        }
        if (endTime.isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.ACTIVITY_TIME_INVALID, "结束时间不能早于当前时间");
        }
    }

    private void assertGoodsExist(Set<Long> goodsIds) {
        List<SkGoods> goods = goodsMapper.selectBatchIds(goodsIds);
        if (goods.size() != goodsIds.size()) {
            Set<Long> found = goods.stream().map(SkGoods::getId).collect(Collectors.toCollection(HashSet::new));
            goodsIds.removeAll(found);
            throw new BizException(ErrorCode.GOODS_NOT_FOUND, "以下商品不存在：" + goodsIds);
        }
    }

    private Map<Long, SkGoods> loadGoods(List<SkActivityGoods> relations) {
        if (relations.isEmpty()) {
            return Map.of();
        }
        Set<Long> goodsIds = relations.stream()
                .map(SkActivityGoods::getGoodsId)
                .collect(Collectors.toSet());
        // 一次批量查询代替逐条 selectById，避免活动详情页的 N+1
        return goodsMapper.selectBatchIds(goodsIds).stream()
                .collect(Collectors.toMap(SkGoods::getId, Function.identity()));
    }

    private Map<Long, Integer> countGoodsByActivity(List<Long> activityIds) {
        if (activityIds.isEmpty()) {
            return Map.of();
        }
        List<SkActivityGoods> relations = activityGoodsMapper.selectList(
                Wrappers.<SkActivityGoods>lambdaQuery()
                        .in(SkActivityGoods::getActivityId, activityIds));
        Map<Long, Integer> map = new HashMap<>();
        for (SkActivityGoods relation : relations) {
            map.merge(relation.getActivityId(), 1, Integer::sum);
        }
        return map;
    }

    private Duration ttlOf(SkActivity activity) {
        Duration ttl = Duration.between(LocalDateTime.now(), activity.getEndTime()).plus(STOCK_TTL_AFTER_END);
        // 活动已过期时 TTL 可能为负，Redis 收到负数会立刻删键，等于没预热
        return ttl.isNegative() ? Duration.ofMinutes(1) : ttl;
    }

    private SkActivity mustGet(Long activityId) {
        SkActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        return activity;
    }

    private ActivityVO toVO(SkActivity activity, int goodsCount) {
        ActivityVO vo = new ActivityVO();
        vo.setId(activity.getId());
        vo.setTitle(activity.getTitle());
        vo.setDescription(activity.getDescription());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(activity.getStatus());
        vo.setStatusDesc(describe(activity.getStatus()));
        vo.setGoodsCount(goodsCount);
        vo.setCreateTime(activity.getCreateTime());
        return vo;
    }

    private String describe(Integer status) {
        ActivityStatus statusEnum = ActivityStatus.of(status);
        return statusEnum == null ? "未知" : statusEnum.getDesc();
    }
}
