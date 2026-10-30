package com.linjian.seckill.domain.service;

import com.linjian.seckill.domain.mapper.SkSeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付超时关单。
 * <p>
 * 秒杀场景里「抢到不付」是常态（占库存的黄牛、抢着玩的用户），不主动关单
 * 会让库存被永久占住。关单后必须同时回补 DB 与 Redis，否则下一次活动会少卖。
 * <p>
 * 扫描条件走 idx_timeout(status, expire_time)，不会退化成全表扫描。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    /** 每轮最多处理多少笔，避免长事务与长时间占用连接 */
    private static final int BATCH_SIZE = 200;

    private final SkSeckillOrderMapper orderMapper;
    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${seckill.order.timeout-scan-interval-ms:30000}",
            initialDelay = 30000)
    public void closeTimeoutOrders() {
        List<Long> ids = orderMapper.selectTimeoutIds(LocalDateTime.now(), BATCH_SIZE);
        if (ids.isEmpty()) {
            return;
        }

        int closed = 0;
        for (Long id : ids) {
            try {
                if (orderService.closeIfTimeout(id, LocalDateTime.now())) {
                    closed++;
                }
            } catch (Exception e) {
                // 单笔失败不能中断整轮，否则一笔坏数据会永久卡住后面所有订单
                log.error("关单失败 orderId={}", id, e);
            }
        }
        log.info("超时关单一轮 扫描={} 关闭={}", ids.size(), closed);
    }
}
