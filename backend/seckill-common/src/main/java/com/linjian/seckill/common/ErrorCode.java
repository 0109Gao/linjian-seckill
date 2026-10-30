package com.linjian.seckill.common;

import lombok.Getter;

/**
 * 业务错误码。
 * <p>
 * 约定：4xxx 为业务错误，5xxx 为系统错误。新增错误码必须在此登记，
 * 不允许在业务代码里直接写魔法数字。
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),

    BAD_REQUEST(400, "参数校验失败"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),

    UNAUTHORIZED(4010, "未登录或登录已过期"),

    STOCK_EMPTY(4001, "商品已售罄"),
    ACTIVITY_CLOSED(4002, "活动未开始或已结束"),
    DUPLICATE_BUY(4003, "您已参与过本次秒杀"),
    TOO_MANY_REQUESTS(4004, "请求过于频繁，请稍后再试"),
    ORDER_NOT_FOUND(4005, "订单不存在"),
    STOCK_NOT_PREHEATED(4006, "库存尚未预热，请稍后再试"),

    GOODS_NOT_FOUND(4101, "商品不存在"),
    ACTIVITY_NOT_FOUND(4102, "活动不存在"),
    ACTIVITY_TIME_INVALID(4103, "活动时间不合法"),
    ACTIVITY_GOODS_EMPTY(4104, "活动至少需要一个商品"),
    ACTIVITY_NOT_EDITABLE(4105, "活动已开始，不允许修改"),
    GOODS_IN_USE(4106, "商品已被活动引用，不能删除"),
    ORDER_STATUS_INVALID(4107, "订单状态不允许该操作"),

    INTERNAL_ERROR(5000, "服务内部错误");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
