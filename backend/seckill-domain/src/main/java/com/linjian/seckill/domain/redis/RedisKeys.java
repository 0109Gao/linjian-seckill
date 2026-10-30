package com.linjian.seckill.domain.redis;

/**
 * Redis 键集中定义。
 * <p>
 * 统一在这里拼键的原因：库存键同时被预热任务、秒杀接口、对账任务、以及
 * 人工排查（redis-cli）使用，任何一处拼错都会表现为「库存永远是 0」这种
 * 极难定位的故障。字符串拼接只允许出现在这个类里。
 */
public final class RedisKeys {

    /** 库存键前缀，位于 db1 */
    public static final String STOCK_PREFIX = "sk:stock:";

    /** 用户已购数量 Hash 前缀，位于 db1；field 是 userId，value 是已购数量 */
    public static final String BOUGHT_PREFIX = "sk:bought:";

    /** 活动已预热商品集合，位于 db1，用于活动结束后精确清理 */
    public static final String PREHEAT_GOODS_PREFIX = "sk:preheat:goods:";

    /** 秒杀请求结果，位于 db1；Hash 结构，字段见 SeckillResultCache */
    public static final String RESULT_PREFIX = "sk:result:";

    /** 活动商品元信息，位于 db1；HashMap，让秒杀链路完全不查库 */
    public static final String META_PREFIX = "sk:meta:";

    /** 用户级频控键前缀，位于 db1 */
    public static final String RATE_LIMIT_PREFIX = "sk:rl:";

    private RedisKeys() {
    }

    public static String stock(Long activityId, Long goodsId) {
        return STOCK_PREFIX + activityId + ":" + goodsId;
    }

    public static String bought(Long activityId, Long goodsId) {
        return BOUGHT_PREFIX + activityId + ":" + goodsId;
    }

    public static String meta(Long activityId, Long goodsId) {
        return META_PREFIX + activityId + ":" + goodsId;
    }

    /** 用户级频控键，TTL 很短，用于挡住脚本刷接口 */
    public static String rateLimit(Long activityId, Long goodsId, Long userId) {
        return RATE_LIMIT_PREFIX + activityId + ":" + goodsId + ":" + userId;
    }

    public static String preheatGoods(Long activityId) {
        return PREHEAT_GOODS_PREFIX + activityId;
    }

    public static String result(Long requestId) {
        return RESULT_PREFIX + requestId;
    }
}
