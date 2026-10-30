package com.linjian.seckill.domain.service;

import com.linjian.seckill.domain.entity.SkLocalMessage;
import com.linjian.seckill.domain.mapper.SkLocalMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息重投任务。
 * <p>
 * 补的是「消息已落库但没送达 Broker」这个窗口：Broker 重启、网络抖动、
 * 甚至应用在 convertAndSend 之后立刻被 kill。没有这个任务，Redis 预扣掉的
 * 库存会永久悬空，用户抢到了却永远等不到订单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMessageRetryTask {

    /** 每轮最多重投多少条，避免一轮把 Broker 打满 */
    private static final int BATCH_SIZE = 100;

    /** 超过这个时间仍是待投递，几乎可以确定有问题，打 ERROR 级别日志 */
    private static final int STUCK_MINUTES = 10;

    private final SkLocalMessageMapper localMessageMapper;
    private final LocalMessageService localMessageService;

    @Scheduled(fixedDelayString = "${seckill.local-message.retry-interval-ms:15000}",
            initialDelay = 15000)
    public void retry() {
        List<SkLocalMessage> pending = localMessageMapper.selectPending(LocalDateTime.now(), BATCH_SIZE);
        if (pending.isEmpty()) {
            return;
        }

        int resent = 0;
        for (SkLocalMessage record : pending) {
            localMessageService.resend(record);
            resent++;

            if (record.getCreateTime() != null
                    && record.getCreateTime().isBefore(LocalDateTime.now().minusMinutes(STUCK_MINUTES))) {
                log.error("消息长时间未投递成功，需人工介入 messageId={} 创建于={} 重试次数={}",
                        record.getMessageId(), record.getCreateTime(), record.getRetryCount());
            }
        }
        log.info("本地消息重投一轮 处理={} 条", resent);
    }
}
