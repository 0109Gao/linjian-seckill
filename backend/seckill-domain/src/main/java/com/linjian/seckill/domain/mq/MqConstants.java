package com.linjian.seckill.domain.mq;

/**
 * 交换机、队列、路由键常量。
 * <p>
 * 集中定义是为了让生产端与消费端引用同一份字面量。这类名字拼错时，
 * 生产者不会报错（消息被交换机丢弃或路由不到），排查成本极高。
 */
public final class MqConstants {

    /** 下单交换机（direct） */
    public static final String ORDER_EXCHANGE = "seckill.order.exchange";

    /** 下单队列 */
    public static final String ORDER_QUEUE = "seckill.order.queue";

    /** 下单路由键 */
    public static final String ORDER_ROUTING_KEY = "seckill.order.create";

    /** 死信交换机：消费失败或消息过期后进入，供人工排查与补偿 */
    public static final String ORDER_DLX_EXCHANGE = "seckill.order.dlx.exchange";

    /** 死信队列 */
    public static final String ORDER_DLQ = "seckill.order.dlq";

    /** 死信路由键 */
    public static final String ORDER_DLQ_ROUTING_KEY = "seckill.order.dead";

    private MqConstants() {
    }
}
