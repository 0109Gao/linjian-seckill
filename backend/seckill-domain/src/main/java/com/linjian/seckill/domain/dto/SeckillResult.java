package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 秒杀请求结果。
 * <p>
 * 前端拿到 PENDING 后轮询，拿到 SUCCESS 才跳订单页，拿到 FAILED 展示原因。
 * 之所以不让接口同步等下单完成：落单要走 DB 事务与乐观锁，把它压进同步链路
 * 会让秒杀接口的 P99 直接取决于数据库，预扣 Redis 换来的吞吐就白费了。
 */
@Data
@Schema(description = "秒杀请求结果")
public class SeckillResult {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    @Schema(description = "状态：PENDING 排队中，SUCCESS 下单成功，FAILED 失败")
    private String status;

    @Schema(description = "秒杀请求流水号，轮询时回传")
    private Long requestId;

    @Schema(description = "订单 ID，仅 SUCCESS 时有值")
    private Long orderId;

    @Schema(description = "订单号，仅 SUCCESS 时有值")
    private String orderNo;

    @Schema(description = "失败错误码，仅 FAILED 时有值")
    private Integer code;

    @Schema(description = "失败原因，仅 FAILED 时有值")
    private String message;

    public static SeckillResult pending(Long requestId) {
        SeckillResult result = new SeckillResult();
        result.setStatus(STATUS_PENDING);
        result.setRequestId(requestId);
        return result;
    }

    public static SeckillResult success(Long requestId, Long orderId, String orderNo) {
        SeckillResult result = new SeckillResult();
        result.setStatus(STATUS_SUCCESS);
        result.setRequestId(requestId);
        result.setOrderId(orderId);
        result.setOrderNo(orderNo);
        return result;
    }

    public static SeckillResult failed(Long requestId, Integer code, String message) {
        SeckillResult result = new SeckillResult();
        result.setStatus(STATUS_FAILED);
        result.setRequestId(requestId);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
