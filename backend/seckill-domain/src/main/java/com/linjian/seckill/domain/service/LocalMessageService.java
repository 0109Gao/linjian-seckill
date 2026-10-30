package com.linjian.seckill.domain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linjian.seckill.domain.entity.SkLocalMessage;
import com.linjian.seckill.domain.mapper.SkLocalMessageMapper;
import com.linjian.seckill.domain.mq.MqConstants;
import com.linjian.seckill.domain.mq.SeckillOrderMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 本地消息表 + 可靠投递。
 * <p>
 * 为什么不能「Lua 扣完直接 convertAndSend」：Redis 预扣是不可回滚的，
 * 一旦 Broker 不可用，扣掉的库存就永久悬空——用户抢到了但永远没有订单。
 * <p>
 * 这里的顺序是：先把消息写进 MySQL（与业务同库），再投递。投递结果通过
 * publisher confirm 异步回写状态；失败的记录由 {@link LocalMessageRetryTask}
 * 定时重投。这样投递语义是 at-least-once，重复由消费端幂等表挡住。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalMessageService {

    private final SkLocalMessageMapper localMessageMapper;

    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 注册投递确认回调。
     * <p>
     * 不用 {@code rabbitTemplate.invoke(...)} 同步等待：confirm 在 Netty 线程里
     * 回调，同步等待会把秒杀接口的 RT 绑到 Broker 的响应时间上，正好抵消异步
     * 下单的意义。异步回写状态 + 定时重投已经足够。
     */
    @PostConstruct
    void registerConfirmCallback() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData == null || correlationData.getId() == null) {
                return;
            }
            String messageId = correlationData.getId();
            try {
                if (ack) {
                    markSent(messageId);
                } else {
                    markFailed(messageId, "broker nack: " + cause);
                }
            } catch (Exception e) {
                // 回调里抛异常会被 Netty 线程吞掉，只能记日志
                log.error("回写投递状态失败 messageId={}", messageId, e);
            }
        });
    }

    /**
     * 落本地消息并立即投递一次。
     * <p>
     * 只有写库失败才会抛异常——此时调用方必须回补 Redis。投递失败不算失败，
     * 记录会留在库里等定时任务重投。
     */
    public void saveAndSend(SeckillOrderMessage message) {
        SkLocalMessage record = new SkLocalMessage();
        record.setMessageId(message.getMessageId());
        record.setExchangeName(MqConstants.ORDER_EXCHANGE);
        record.setRoutingKey(MqConstants.ORDER_ROUTING_KEY);
        record.setPayload(toJson(message));
        record.setStatus(SkLocalMessage.STATUS_PENDING);
        record.setRetryCount(0);
        localMessageMapper.insert(record);

        send(record, message);
    }

    /**
     * 供定时重投任务使用。
     */
    public void resend(SkLocalMessage record) {
        try {
            SeckillOrderMessage message = objectMapper.readValue(record.getPayload(), SeckillOrderMessage.class);
            send(record, message);
        } catch (Exception e) {
            log.error("重投失败 messageId={}", record.getMessageId(), e);
            markFailed(record.getMessageId(), e.getMessage());
        }
    }

    private void send(SkLocalMessage record, SeckillOrderMessage message) {
        MessagePostProcessor setMessageId = msg -> {
            msg.getMessageProperties().setMessageId(record.getMessageId());
            return msg;
        };
        try {
            rabbitTemplate.convertAndSend(
                    record.getExchangeName(),
                    record.getRoutingKey(),
                    message,
                    setMessageId,
                    new CorrelationData(record.getMessageId()));
        } catch (Exception e) {
            // Broker 不可用：标记待重试，不抛给调用方（Redis 已扣，抛出去也没法回滚）
            log.warn("消息投递失败，将定时重投 messageId={} cause={}", record.getMessageId(), e.getMessage());
            markFailed(record.getMessageId(), e.getMessage());
        }
    }

    private void markSent(String messageId) {
        SkLocalMessage update = new SkLocalMessage();
        update.setStatus(SkLocalMessage.STATUS_SENT);
        update.setNextRetryTime(null);
        localMessageMapper.update(update, Wrappers.<SkLocalMessage>lambdaUpdate()
                .eq(SkLocalMessage::getMessageId, messageId));
    }

    /**
     * 标记失败并安排退避重试。
     * <p>
     * 退避间隔按重试次数线性增长，上限 5 分钟。不设上限的话，一条永远投不出去
     * 的消息会以 30 秒一次的频率无限重试，把日志刷满。
     */
    private void markFailed(String messageId, String cause) {
        SkLocalMessage record = localMessageMapper.selectOne(Wrappers.<SkLocalMessage>lambdaQuery()
                .eq(SkLocalMessage::getMessageId, messageId));
        if (record == null) {
            return;
        }
        int retry = record.getRetryCount() == null ? 0 : record.getRetryCount();
        long delaySeconds = Math.min(30L * (retry + 1), 300L);

        SkLocalMessage update = new SkLocalMessage();
        update.setStatus(SkLocalMessage.STATUS_FAILED);
        update.setRetryCount(retry + 1);
        update.setNextRetryTime(LocalDateTime.now().plusSeconds(delaySeconds));
        localMessageMapper.update(update, Wrappers.<SkLocalMessage>lambdaUpdate()
                .eq(SkLocalMessage::getMessageId, messageId));
        log.warn("消息投递失败 messageId={} retry={} next={}s cause={}",
                messageId, retry + 1, delaySeconds, cause);
    }

    private String toJson(SeckillOrderMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            // 序列化失败属于编码错误，必须让调用方回补库存
            throw new IllegalStateException("下单消息序列化失败", e);
        }
    }
}
