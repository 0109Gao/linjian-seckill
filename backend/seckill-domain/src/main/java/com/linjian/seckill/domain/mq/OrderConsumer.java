package com.linjian.seckill.domain.mq;

import com.linjian.seckill.domain.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 下单消息消费者。
 * <p>
 * 手动 ack 而不是自动 ack：自动 ack 在方法抛异常时消息已经被确认，等于丢消息。
 * <p>
 * 重试策略只放行一次：首次失败重回队列，第二次失败进死信队列。不设上限的话，
 * 一条注定失败的消息会在队列里无限循环，把 CPU 和日志打满，还会阻塞后面的消息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = MqConstants.ORDER_QUEUE)
    public void onMessage(SeckillOrderMessage message, Message raw, Channel channel) throws IOException {
        long deliveryTag = raw.getMessageProperties().getDeliveryTag();
        boolean redelivered = raw.getMessageProperties().isRedelivered();
        try {
            orderService.handleMessage(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("下单消息消费失败 messageId={} requestId={} redelivered={}",
                    message.getMessageId(), message.getRequestId(), redelivered, e);
            // 首次失败重回队列（可能是数据库瞬时不可用），第二次直接进死信
            channel.basicNack(deliveryTag, false, !redelivered);
        }
    }
}
