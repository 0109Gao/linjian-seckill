package com.linjian.seckill.domain.enums;

import lombok.Getter;

/**
 * 订单状态机。
 * <p>
 * 0 待支付 --支付--> 1 已支付
 * 0 待支付 --超时/主动取消--> 2 已取消 --回补库存--> 3 已回补
 * <p>
 * 「已取消」与「已回补」分开是刻意的：关单只改状态，回补库存是另一件事，
 * 中间可能失败（Redis 键已过期、DB 扣减被乐观锁挡掉）。如果合成一个状态，
 * 回补失败就再也查不出是哪些订单欠着库存。
 */
@Getter
public enum OrderStatus {

    /** 已落单，等待支付 */
    PENDING(0, "待支付"),

    /** 已支付，库存确定被消耗 */
    PAID(1, "已支付"),

    /** 已取消（超时未支付或用户主动取消），库存尚未回补 */
    CANCELLED(2, "已取消"),

    /** 库存已回补完成 */
    RESTORED(3, "已关闭");

    private final Integer code;
    private final String desc;

    OrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
