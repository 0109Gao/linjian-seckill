package com.linjian.seckill.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务配置。
 * <p>
 * 显式指定线程池而不是用默认的单线程调度器：预热扫描与后续的关单、对账
 * 任务会共存，单线程下只要有一个任务卡住（比如对账扫了全表），其它任务
 * 全部延后，这种问题在压测时非常难看出来。
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("sk-sched-");
        // 关闭时等待正在执行的任务结束，避免库存扣减到一半被中断
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);
        return scheduler;
    }
}
