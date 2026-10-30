package com.linjian.seckill.domain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linjian.seckill.domain.entity.SkActivity;
import com.linjian.seckill.domain.entity.SkActivityGoods;
import com.linjian.seckill.domain.enums.ActivityStatus;
import com.linjian.seckill.domain.mapper.SkActivityGoodsMapper;
import com.linjian.seckill.domain.mapper.SkActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Redis 与 DB 的库存对账。
 * <p>
 * 对两种偏差的处理**刻意不对称**，这是本任务最关键的设计：
 * <ul>
 *   <li>{@code Redis > DB}：Redis 里还有库存但数据库已经卖完了，继续放行就是<b>超卖</b>。
 *       必须立即把 Redis 修正到 DB 值。这是唯一需要自动修复的方向。</li>
 *   <li>{@code Redis < DB}：可能是消息还在队列里没落单（在途），也可能真的丢了消息。
 *       自动把 Redis 补回 DB 值会覆盖掉在途扣减，反而造成超卖。所以只告警、不修改，
 *       把判断交给人工——这就是「对账可以少卖，不可以多卖」。</li>
 * </ul>
 * DB 是最终事实来源：库存扣减、订单落库都在 DB 事务里，Redis 只是性能层。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockReconcileTask {

    private final SkActivityMapper activityMapper;
    private final SkActivityGoodsMapper activityGoodsMapper;
    private final StockRedisService stockRedisService;

    @Scheduled(fixedDelayString = "${seckill.reconcile.interval-ms:300000}", initialDelay = 60000)
    public void reconcile() {
        List<SkActivity> activities = activityMapper.selectList(Wrappers.<SkActivity>lambdaQuery()
                .in(SkActivity::getStatus, ActivityStatus.PENDING.getCode(), ActivityStatus.RUNNING.getCode())
                .gt(SkActivity::getEndTime, LocalDateTime.now()));
        if (activities.isEmpty()) {
            return;
        }

        int checked = 0;
        int overSold = 0;
        int shortSell = 0;

        for (SkActivity activity : activities) {
            List<SkActivityGoods> relations = activityGoodsMapper.selectList(
                    Wrappers.<SkActivityGoods>lambdaQuery()
                            .eq(SkActivityGoods::getActivityId, activity.getId()));
            for (SkActivityGoods relation : relations) {
                Long redisRemain = stockRedisService.remaining(activity.getId(), relation.getGoodsId());
                if (redisRemain == null) {
                    // 未预热，不参与对账
                    continue;
                }
                checked++;
                int dbStock = relation.getStock();
                long diff = redisRemain - dbStock;

                if (diff > 0) {
                    log.error("对账发现超卖风险 activityId={} goodsId={} redis={} db={} 差={}，修正 Redis",
                            activity.getId(), relation.getGoodsId(), redisRemain, dbStock, diff);
                    if (stockRedisService.resetStock(activity.getId(), relation.getGoodsId(), dbStock)) {
                        overSold++;
                    }
                } else if (diff < 0) {
                    // 不修正，只告警。在途消息落单后差额会自然消失
                    log.warn("对账发现库存缺口 activityId={} goodsId={} redis={} db={} 差={}（可能为在途消息）",
                            activity.getId(), relation.getGoodsId(), redisRemain, dbStock, diff);
                    shortSell++;
                }
            }
        }

        if (overSold > 0 || shortSell > 0) {
            log.warn("对账完成 检查={} 修正超卖={} 告警缺口={}", checked, overSold, shortSell);
        } else if (checked > 0) {
            log.info("对账完成 检查={} 全部一致", checked);
        }
    }
}
