package com.linjian.seckill.domain.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * 下单消息体。
 * <p>
 * 只带 ID 与数量，不带价格与商品名：价格必须由消费端在事务里重新读库，
 * 否则消息一旦被篡改或延迟，用户就能用旧价格下单。这是「消息只传指令，
 * 不传事实」的原则。
 */
@Data
public class SeckillOrderMessage implements Serializable {

    /** 消息唯一标识，消费端据此写幂等表 */
    private String messageId;

    /** 秒杀请求流水号，前端用它轮询结果 */
    private Long requestId;

    private Long userId;

    private Long activityId;

    private Long activityGoodsId;

    private Long goodsId;

    private Integer quantity;
}
