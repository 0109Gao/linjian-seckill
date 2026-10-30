package com.linjian.seckill.domain.config;

import com.linjian.seckill.domain.mq.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 下单链路的消息拓扑。
 * <pre>
 *   producer ──► seckill.order.exchange ──(seckill.order.create)──► seckill.order.queue
 *                                                                        │ 消费失败/拒收
 *                                                                        ▼
 *                                              seckill.order.dlx.exchange ──► seckill.order.dlq
 * </pre>
 * 声明式创建（队列/交换机不存在时自动建）在本地开发足够；生产环境应由运维
 * 单独管理拓扑，避免应用启动权限过大。
 */
@Configuration
public class RabbitMQConfig {

    /** 下单交换机。direct 而非 fanout：后续要加「积分」「通知」队列时只需新增绑定 */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(MqConstants.ORDER_EXCHANGE, true, false);
    }

    /**
     * 下单队列。
     * <p>
     * 绑定死信交换机是「消息不丢」的关键一环：消费者主动 reject 且不重回队列，
     * 或消息 TTL 到期，都会转到死信队列，而不是凭空消失。
     */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(MqConstants.ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", MqConstants.ORDER_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstants.ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange orderDlxExchange() {
        return new DirectExchange(MqConstants.ORDER_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderDlq() {
        return QueueBuilder.durable(MqConstants.ORDER_DLQ).build();
    }

    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(MqConstants.ORDER_ROUTING_KEY);
    }

    @Bean
    public Binding orderDlqBinding() {
        return BindingBuilder.bind(orderDlq()).to(orderDlxExchange()).with(MqConstants.ORDER_DLQ_ROUTING_KEY);
    }

    /**
     * JSON 序列化。默认的 SimpleMessageConverter 要求消息体可序列化，
     * 一旦 DTO 加字段而消费端还是旧版本，会直接抛出反序列化异常。
     * JSON 在跨版本、人工排查（管理页可读）两方面都更好。
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
