package com.linjian.seckill.domain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linjian.seckill.domain.entity.SkActivity;
import com.linjian.seckill.domain.enums.ActivityStatus;
import com.linjian.seckill.domain.mapper.SkActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动状态推进与库存预热任务。
 * <p>
 * 为什么用「定时扫描」而不是「启动时全量预热」：
 * 1. 全量预热把预热动作和重启绑在一起，重启一次就回写一次库存，若此时活动
 *    正在进行，已扣减的库存会被重置（SETNX 能挡住，但状态推进会重复）；
 * 2. 活动多时启动时间不可控，而预热本身只需要在开始前几分钟完成；
 * 3. 扫描式预热可以按 start_time 索引只捞「即将开始」的少数活动。
 * <p>
 * 代价是库存可用时间取决于扫描间隔（默认 30 秒），因此提前量给到 5 分钟，
 * 足够覆盖抖动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreheatScheduler {

    private final SkActivityMapper activityMapper;
    private final ActivityService activityService;

    /** 提前多少分钟预热 */
    @Value("${seckill.preheat.lead-minutes:5}")
    private int leadMinutes;

    @Scheduled(fixedDelayString = "${seckill.preheat.scan-interval-ms:30000}")
    public void scan() {
        LocalDateTime now = LocalDateTime.now();
        int preheated = preheatDueActivities(now);
        int finished = finishExpiredActivities(now);
        if (preheated > 0 || finished > 0) {
            log.info("预热任务一轮完成：预热活动 {} 个，结束活动 {} 个", preheated, finished);
        }
    }

    /**
     * 预热「即将开始」的活动。
     */
    private int preheatDueActivities(LocalDateTime now) {
        List<SkActivity> due = activityMapper.selectList(
                Wrappers.<SkActivity>lambdaQuery()
                        .eq(SkActivity::getStatus, ActivityStatus.PENDING.getCode())
                        .le(SkActivity::getStartTime, now.plusMinutes(leadMinutes))
                        .orderByAsc(SkActivity::getStartTime));

        int count = 0;
        for (SkActivity activity : due) {
            try {
                // 只统计「真的写入了库存」的活动：每轮扫描都会命中的已预热活动
                // 若也计入，日志会天天报「预热 N 个」却看不到任何实际动作
                int written = activityService.doPreheat(activity);
                if (written > 0) {
                    count++;
                }
            } catch (Exception e) {
                // 单个活动失败不能中断整轮扫描，否则一个坏数据会永久卡住其它活动
                log.error("活动预热失败 activityId={}", activity.getId(), e);
            }
        }
        return count;
    }

    /**
     * 结束到点的活动并清理库存键。
     */
    private int finishExpiredActivities(LocalDateTime now) {
        List<SkActivity> expired = activityMapper.selectList(
                Wrappers.<SkActivity>lambdaQuery()
                        .eq(SkActivity::getStatus, ActivityStatus.RUNNING.getCode())
                        .le(SkActivity::getEndTime, now));

        int count = 0;
        for (SkActivity activity : expired) {
            try {
                activityService.finish(activity);
                count++;
            } catch (Exception e) {
                log.error("活动结束处理失败 activityId={}", activity.getId(), e);
            }
        }
        return count;
    }
}
