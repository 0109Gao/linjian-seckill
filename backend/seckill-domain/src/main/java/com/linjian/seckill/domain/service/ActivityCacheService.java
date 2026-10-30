package com.linjian.seckill.domain.service;

import com.linjian.seckill.domain.config.StockRedisTemplateHolder;
import com.linjian.seckill.domain.dto.ActivityGoodsMeta;
import com.linjian.seckill.domain.entity.SkActivity;
import com.linjian.seckill.domain.entity.SkActivityGoods;
import com.linjian.seckill.domain.entity.SkGoods;
import com.linjian.seckill.domain.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 活动商品元信息缓存（db1）。
 * <p>
 * 秒杀接口的性能上限取决于「同步链路里有几次网络往返」。把活动、商品、价格、
 * 限购全部预热进 Redis 后，一次秒杀只需要：读元信息 → 频控 → Lua 扣减，
 * 三次 Redis 往返，零次数据库访问。这是单机扛住高并发的前提。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityCacheService {

    private static final String F_ACTIVITY_GOODS_ID = "activityGoodsId";
    private static final String F_GOODS_NAME = "goodsName";
    private static final String F_PRICE = "price";
    private static final String F_LIMIT = "limitPerUser";
    private static final String F_START = "startTime";
    private static final String F_END = "endTime";

    private final StockRedisTemplateHolder holder;

    /**
     * 写入元信息。与库存键使用同一个 TTL，保证两者同时过期，
     * 不会出现「库存没了但元信息还在」的半残状态。
     */
    public void warm(SkActivity activity, SkActivityGoods relation, SkGoods goods, Duration ttl) {
        String key = RedisKeys.meta(activity.getId(), relation.getGoodsId());
        StringRedisTemplate redis = holder.template();
        redis.opsForHash().putAll(key, Map.of(
                F_ACTIVITY_GOODS_ID, String.valueOf(relation.getId()),
                F_GOODS_NAME, goods == null ? "" : goods.getName(),
                F_PRICE, relation.getSeckillPrice().toPlainString(),
                F_LIMIT, String.valueOf(relation.getLimitPerUser()),
                F_START, String.valueOf(toMillis(activity.getStartTime())),
                F_END, String.valueOf(toMillis(activity.getEndTime()))));
        redis.expire(key, ttl);
    }

    /**
     * 读取元信息。
     *
     * @return null 表示未预热（或活动已结束被清理），调用方应返回「尚未开始」
     */
    public ActivityGoodsMeta get(Long activityId, Long goodsId) {
        Map<Object, Object> entries = holder.template().opsForHash().entries(RedisKeys.meta(activityId, goodsId));
        if (entries.isEmpty()) {
            return null;
        }

        ActivityGoodsMeta meta = new ActivityGoodsMeta();
        meta.setActivityId(activityId);
        meta.setGoodsId(goodsId);
        meta.setActivityGoodsId(Long.parseLong(str(entries.get(F_ACTIVITY_GOODS_ID))));
        meta.setGoodsName(str(entries.get(F_GOODS_NAME)));
        meta.setPrice(str(entries.get(F_PRICE)));
        meta.setLimitPerUser(Integer.parseInt(str(entries.get(F_LIMIT))));
        meta.setStartTime(Long.parseLong(str(entries.get(F_START))));
        meta.setEndTime(Long.parseLong(str(entries.get(F_END))));
        return meta;
    }

    public void evict(Long activityId, Long goodsId) {
        holder.template().delete(RedisKeys.meta(activityId, goodsId));
    }

    private long toMillis(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }
}
