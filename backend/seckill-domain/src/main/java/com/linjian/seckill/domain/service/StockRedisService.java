package com.linjian.seckill.domain.service;

import com.linjian.seckill.domain.config.StockRedisTemplateHolder;
import com.linjian.seckill.domain.redis.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 库存 Redis 操作。所有库存键都在 db1，与登录态所在的 db0 隔离。
 * <p>
 * 这个类不做任何业务判断（活动是否开始、商品是否存在），只负责「把库存数字
 * 安全地从 Redis 里搬出来」。活动状态校验在 SeckillService 里做，职责分开
 * 才好单独压测与替换。
 */
@Slf4j
@Service
public class StockRedisService {

    /** 库存键不存在：活动未预热 */
    public static final long NOT_PREHEATED = -1L;

    /** 库存不足 */
    public static final long INSUFFICIENT = -2L;

    /** 超出每人限购 */
    public static final long LIMIT_EXCEEDED = -3L;

    private static final RedisScript<Long> DEDUCT_SCRIPT =
            RedisScript.of(new ClassPathResource("lua/seckill_stock.lua"), Long.class);

    private final StringRedisTemplate redis;

    public StockRedisService(StockRedisTemplateHolder holder) {
        this.redis = holder.template();
    }

    /**
     * 预热单个商品库存。
     * <p>
     * 用 SETNX 而不是 SET：这个方法是幂等的但库存不是。活动进行中服务重启、
     * 或定时任务重复触发时，如果无条件 SET，已扣掉的库存会被重置回初始值，
     * 直接变成超卖。SETNX 保证「只在键不存在时写入」。
     *
     * @return true 表示本次真的写入了，false 表示键已存在（此前预热过）
     */
    public boolean preheat(Long activityId, Long goodsId, int stock, Duration ttl) {
        String key = RedisKeys.stock(activityId, goodsId);
        Boolean written = redis.opsForValue().setIfAbsent(key, String.valueOf(stock), ttl);
        if (Boolean.TRUE.equals(written)) {
            // 记录到活动维度集合，活动结束后按集合精确清理，
            // 不用 KEYS/SCAN 全库匹配
            redis.opsForSet().add(RedisKeys.preheatGoods(activityId), String.valueOf(goodsId));
            redis.expire(RedisKeys.preheatGoods(activityId), ttl);
            log.info("库存预热完成 activityId={} goodsId={} stock={}", activityId, goodsId, stock);
            return true;
        }
        return false;
    }

    /**
     * 读取剩余库存。
     *
     * @return null 表示未预热
     */
    public Long remaining(Long activityId, Long goodsId) {
        String value = redis.opsForValue().get(RedisKeys.stock(activityId, goodsId));
        return value == null ? null : Long.parseLong(value);
    }

    /**
     * 读取某用户在某活动商品上已购买的数量。
     */
    public int boughtCount(Long activityId, Long goodsId, Long userId) {
        Object value = redis.opsForHash().get(RedisKeys.bought(activityId, goodsId), String.valueOf(userId));
        return value == null ? 0 : Integer.parseInt(value.toString());
    }

    /**
     * 原子预扣库存，同时完成限购校验与「记录用户已购」。
     * <p>
     * 这三件事必须在同一个 Lua 里做完：分开执行时，同一用户的两个并发请求会
     * 双双通过限购校验，库存也会被读到同一份余量，两者都是超卖。
     *
     * @return >=0 扣减成功后的剩余库存；{@link #NOT_PREHEATED} 未预热；
     *         {@link #INSUFFICIENT} 库存不足；{@link #LIMIT_EXCEEDED} 超出限购
     */
    public long deduct(Long activityId, Long goodsId, Long userId, int quantity, int limitPerUser) {
        Long result = redis.execute(
                DEDUCT_SCRIPT,
                List.of(RedisKeys.stock(activityId, goodsId), RedisKeys.bought(activityId, goodsId)),
                String.valueOf(quantity),
                String.valueOf(userId),
                String.valueOf(limitPerUser));
        return result == null ? NOT_PREHEATED : result;
    }

    /**
     * 回补库存与用户已购计数。下单失败、超时关单时调用。
     * <p>
     * 只在库存键存在时回补：活动已经结束并被清理的情况下再 INCRBY 会凭空造出一个
     * 「没有 TTL 的库存键」，下次活动复用同一个 activityId 时会读到脏数据。
     * 用户已购计数必须一起回退，否则用户取消订单后再也抢不到同一商品。
     */
    public void restore(Long activityId, Long goodsId, Long userId, int quantity) {
        String stockKey = RedisKeys.stock(activityId, goodsId);
        if (!Boolean.TRUE.equals(redis.hasKey(stockKey))) {
            log.warn("库存键已不存在，跳过回补 activityId={} goodsId={} quantity={}",
                    activityId, goodsId, quantity);
            return;
        }
        redis.opsForValue().increment(stockKey, quantity);

        String boughtKey = RedisKeys.bought(activityId, goodsId);
        Long left = redis.opsForHash().increment(boughtKey, String.valueOf(userId), -quantity);
        if (left != null && left <= 0) {
            // 归零就删掉 field，避免 Hash 里留一堆 0 值把内存撑大
            redis.opsForHash().delete(boughtKey, String.valueOf(userId));
        }
    }

    /**
     * 把库存键重置为指定值，并保留原 TTL（对账修正用）。
     * <p>
     * 不用 {@code SET ... KEEPTTL}：本地 Redis 是 5.0.14，KEEPTTL 需要 6.0+。
     * 所以先读 TTL 再设值，TTL 已丢失的键不修正——宁可少改，也不造出一个
     * 永不过期的库存键。
     *
     * @return true 表示修正成功
     */
    public boolean resetStock(Long activityId, Long goodsId, int value) {
        String key = RedisKeys.stock(activityId, goodsId);
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            log.warn("库存键无有效 TTL，放弃修正 activityId={} goodsId={} ttl={}", activityId, goodsId, ttl);
            return false;
        }
        redis.opsForValue().set(key, String.valueOf(value), Duration.ofSeconds(ttl));
        return true;
    }

    /**
     * 清理某个活动的全部库存键与已购计数（活动结束或取消时调用）。
     */
    public void clearActivity(Long activityId) {
        String setKey = RedisKeys.preheatGoods(activityId);
        Set<String> goodsIds = redis.opsForSet().members(setKey);
        if (goodsIds != null && !goodsIds.isEmpty()) {
            List<String> keys = goodsIds.stream()
                    .flatMap(goodsId -> {
                        long id = Long.parseLong(goodsId);
                        return List.of(RedisKeys.stock(activityId, id),
                                RedisKeys.bought(activityId, id)).stream();
                    })
                    .toList();
            redis.delete(keys);
        }
        redis.delete(setKey);
        log.info("已清理活动库存键 activityId={} 商品数={}", activityId, goodsIds == null ? 0 : goodsIds.size());
    }
}
