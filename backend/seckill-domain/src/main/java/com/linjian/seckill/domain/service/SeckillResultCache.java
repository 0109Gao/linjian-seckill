package com.linjian.seckill.domain.service;

import com.linjian.seckill.domain.config.StockRedisTemplateHolder;
import com.linjian.seckill.domain.dto.SeckillResult;
import com.linjian.seckill.domain.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 秒杀结果的短期缓存（db1）。
 * <p>
 * 为什么结果要放 Redis 而不是直接查订单表：刚抢完的那一秒有大量客户端在轮询，
 * 每次都查 MySQL 会把「异步下单省下来的数据库压力」又还回去。结果只在 Redis
 * 里保留 30 分钟，超时后回落到订单表查询。
 */
@Service
@RequiredArgsConstructor
public class SeckillResultCache {

    /** 结果保留时长，覆盖用户从抢购到支付完成的窗口 */
    private static final Duration TTL = Duration.ofMinutes(30);

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ORDER_ID = "orderId";
    private static final String FIELD_ORDER_NO = "orderNo";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_MESSAGE = "message";

    private final StockRedisTemplateHolder holder;

    public void markPending(Long requestId) {
        StringRedisTemplate redis = holder.template();
        String key = RedisKeys.result(requestId);
        redis.opsForHash().put(key, FIELD_STATUS, SeckillResult.STATUS_PENDING);
        redis.expire(key, TTL);
    }

    public void markSuccess(Long requestId, Long orderId, String orderNo) {
        StringRedisTemplate redis = holder.template();
        String key = RedisKeys.result(requestId);
        redis.opsForHash().put(key, FIELD_STATUS, SeckillResult.STATUS_SUCCESS);
        redis.opsForHash().put(key, FIELD_ORDER_ID, String.valueOf(orderId));
        redis.opsForHash().put(key, FIELD_ORDER_NO, orderNo);
        redis.expire(key, TTL);
    }

    public void markFailed(Long requestId, Integer code, String message) {
        StringRedisTemplate redis = holder.template();
        String key = RedisKeys.result(requestId);
        redis.opsForHash().put(key, FIELD_STATUS, SeckillResult.STATUS_FAILED);
        redis.opsForHash().put(key, FIELD_CODE, String.valueOf(code));
        redis.opsForHash().put(key, FIELD_MESSAGE, message);
        redis.expire(key, TTL);
    }

    /**
     * 读取结果。
     *
     * @return null 表示没有这条请求的记录（未发起或已过期），调用方应回落到 DB 查询
     */
    public SeckillResult get(Long requestId) {
        StringRedisTemplate redis = holder.template();
        var entries = redis.opsForHash().entries(RedisKeys.result(requestId));
        if (entries.isEmpty()) {
            return null;
        }

        SeckillResult result = new SeckillResult();
        result.setRequestId(requestId);
        result.setStatus(str(entries.get(FIELD_STATUS)));

        String orderId = str(entries.get(FIELD_ORDER_ID));
        if (orderId != null) {
            result.setOrderId(Long.parseLong(orderId));
        }
        result.setOrderNo(str(entries.get(FIELD_ORDER_NO)));

        String code = str(entries.get(FIELD_CODE));
        if (code != null) {
            result.setCode(Integer.parseInt(code));
        }
        result.setMessage(str(entries.get(FIELD_MESSAGE)));
        return result;
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }
}
