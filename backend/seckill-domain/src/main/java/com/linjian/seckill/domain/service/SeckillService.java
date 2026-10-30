package com.linjian.seckill.domain.service;

import com.linjian.seckill.common.BizException;
import com.linjian.seckill.common.ErrorCode;
import com.linjian.seckill.common.SnowflakeIdGenerator;
import com.linjian.seckill.domain.config.StockRedisTemplateHolder;
import com.linjian.seckill.domain.dto.ActivityGoodsMeta;
import com.linjian.seckill.domain.dto.SeckillResult;
import com.linjian.seckill.domain.entity.SkSeckillOrder;
import com.linjian.seckill.domain.mq.SeckillOrderMessage;
import com.linjian.seckill.domain.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 秒杀入口。
 * <p>
 * 同步链路只有三次 Redis 往返：读元信息 → 用户级频控 → Lua 原子预扣，
 * 全程不访问 MySQL。落单走 RabbitMQ 异步完成，接口返回 requestId 由前端轮询。
 * 这是「预扣即扣」方案的入口半边，另半边在 {@link OrderService}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillService {

    /** 用户级频控窗口。1 秒足够挡住脚本连点，又不会误伤网络抖动后的正常重试 */
    private static final Duration RATE_LIMIT_TTL = Duration.ofSeconds(1);

    /** 单次购买数量。当前业务是「一件一单」，多做数量会引入部分成交的复杂度 */
    private static final int QUANTITY = 1;

    private final ActivityCacheService activityCacheService;
    private final StockRedisService stockRedisService;
    private final SeckillResultCache resultCache;
    private final LocalMessageService localMessageService;
    private final OrderService orderService;
    private final StockRedisTemplateHolder redisHolder;

    /**
     * 发起秒杀。
     *
     * @return 排队中的结果（含 requestId），调用方需轮询 {@link #queryResult}
     */
    public SeckillResult seckill(Long activityId, Long goodsId, Long userId) {
        // 1. 元信息：预热时写入，不存在说明活动还没开始预热或已结束清理
        ActivityGoodsMeta meta = activityCacheService.get(activityId, goodsId);
        if (meta == null) {
            throw new BizException(ErrorCode.STOCK_NOT_PREHEATED);
        }
        assertActivityOpen(meta);

        // 2. 用户级频控。放在 Lua 之前，让刷接口的请求在这里就被挡住，
        //    不必消耗一次脚本调用
        String rateKey = RedisKeys.rateLimit(activityId, goodsId, userId);
        Boolean acquired = redisHolder.template().opsForValue()
                .setIfAbsent(rateKey, "1", RATE_LIMIT_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS);
        }

        // 3. 生成请求流水号并先落一个 PENDING，保证前端立刻轮询也能拿到状态
        Long requestId = SnowflakeIdGenerator.getInstance().nextId();
        resultCache.markPending(requestId);

        // 4. Lua 原子预扣（库存校验 + 限购校验 + 扣减 + 记录用户，四步不可分割）
        long remain = stockRedisService.deduct(activityId, goodsId, userId, QUANTITY, meta.getLimitPerUser());
        if (remain == StockRedisService.NOT_PREHEATED) {
            resultCache.markFailed(requestId, ErrorCode.STOCK_NOT_PREHEATED.getCode(),
                    ErrorCode.STOCK_NOT_PREHEATED.getMessage());
            throw new BizException(ErrorCode.STOCK_NOT_PREHEATED);
        }
        if (remain == StockRedisService.INSUFFICIENT) {
            resultCache.markFailed(requestId, ErrorCode.STOCK_EMPTY.getCode(), ErrorCode.STOCK_EMPTY.getMessage());
            throw new BizException(ErrorCode.STOCK_EMPTY);
        }
        if (remain == StockRedisService.LIMIT_EXCEEDED) {
            // 已经买过。可能是重复提交（网络超时后重试），也可能是脚本刷。
            // 查一次订单：真有单就把结果直接给他，别让用户以为白抢了
            SkSeckillOrder existing = orderService.findByUserAndGoods(activityId, goodsId, userId);
            if (existing != null) {
                resultCache.markSuccess(requestId, existing.getId(), existing.getOrderNo());
                return SeckillResult.success(requestId, existing.getId(), existing.getOrderNo());
            }
            resultCache.markFailed(requestId, ErrorCode.DUPLICATE_BUY.getCode(),
                    ErrorCode.DUPLICATE_BUY.getMessage());
            throw new BizException(ErrorCode.DUPLICATE_BUY);
        }

        // 5. 落本地消息并投递。写库失败必须回补 Redis，否则库存永久悬空
        SeckillOrderMessage message = buildMessage(requestId, activityId, goodsId, userId, meta);
        try {
            localMessageService.saveAndSend(message);
        } catch (Exception e) {
            log.error("下单消息落库失败，回补库存 requestId={}", requestId, e);
            stockRedisService.restore(activityId, goodsId, userId, QUANTITY);
            resultCache.markFailed(requestId, ErrorCode.INTERNAL_ERROR.getCode(), "下单请求提交失败，请重试");
            throw new BizException(ErrorCode.INTERNAL_ERROR, "下单请求提交失败，请重试");
        }

        log.info("预扣成功 requestId={} userId={} activityId={} goodsId={} remain={}",
                requestId, userId, activityId, goodsId, remain);
        return SeckillResult.pending(requestId);
    }

    /**
     * 轮询下单结果。
     * <p>
     * 先查 Redis（30 分钟内有效），没有再回落到订单表——用户可能半小时后才
     * 打开订单页，此时缓存早已过期。
     */
    public SeckillResult queryResult(Long requestId, Long userId) {
        SeckillResult cached = resultCache.get(requestId);
        if (cached != null) {
            return cached;
        }
        SkSeckillOrder order = orderService.findByRequestId(requestId);
        if (order == null) {
            // 缓存过期且库里没有：可能仍在排队（消息重投中），也可能请求根本不存在。
            // 返回 PENDING 让前端继续轮询，比直接报错体验好，也避免误导用户重复提交
            return SeckillResult.pending(requestId);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return SeckillResult.success(requestId, order.getId(), order.getOrderNo());
    }

    private void assertActivityOpen(ActivityGoodsMeta meta) {
        long now = System.currentTimeMillis();
        if (now < meta.getStartTime()) {
            throw new BizException(ErrorCode.ACTIVITY_CLOSED, "活动尚未开始");
        }
        if (now > meta.getEndTime()) {
            throw new BizException(ErrorCode.ACTIVITY_CLOSED, "活动已结束");
        }
    }

    private SeckillOrderMessage buildMessage(Long requestId, Long activityId, Long goodsId,
                                             Long userId, ActivityGoodsMeta meta) {
        SeckillOrderMessage message = new SeckillOrderMessage();
        // messageId 用 UUID 而不是 requestId：requestId 是业务流水，messageId 是
        // 传输层标识，两者生命周期不同（消息可能被重投多次，requestId 只有一个）
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        message.setRequestId(requestId);
        message.setUserId(userId);
        message.setActivityId(activityId);
        message.setActivityGoodsId(meta.getActivityGoodsId());
        message.setGoodsId(goodsId);
        message.setQuantity(QUANTITY);
        return message;
    }
}
