package com.linjian.seckill.domain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linjian.seckill.domain.dto.ActivityStatsRow;
import com.linjian.seckill.domain.dto.ActivityStatsVO;
import com.linjian.seckill.domain.dto.DashboardOverviewVO;
import com.linjian.seckill.domain.dto.OrderStatusRow;
import com.linjian.seckill.domain.entity.SkActivity;
import com.linjian.seckill.domain.enums.ActivityStatus;
import com.linjian.seckill.domain.mapper.SkActivityMapper;
import com.linjian.seckill.domain.mapper.SkGoodsMapper;
import com.linjian.seckill.domain.mapper.SkStatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 监控看板。
 * <p>
 * 数据全部来自数据库的一次性聚合，不读 Redis：看板是给人「盘点」用的，
 * 需要的是与订单表自洽的账，而不是某一瞬间的缓存快照。Redis 余量在
 * 库存页单独看，那里才关心「现在还能抢多少」。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SkActivityMapper activityMapper;
    private final SkGoodsMapper goodsMapper;
    private final SkStatsMapper statsMapper;

    public DashboardOverviewVO overview(int activityLimit) {
        DashboardOverviewVO vo = new DashboardOverviewVO();

        vo.setActivityTotal(activityMapper.selectCount(null));
        vo.setActivityRunning(activityMapper.selectCount(
                Wrappers.<SkActivity>lambdaQuery().eq(SkActivity::getStatus, ActivityStatus.RUNNING.getCode())));
        vo.setGoodsTotal(goodsMapper.selectCount(null));

        long pending = 0;
        long paid = 0;
        long closed = 0;
        BigDecimal salesAmount = BigDecimal.ZERO;

        for (OrderStatusRow row : statsMapper.orderStats()) {
            long cnt = row.getCnt() == null ? 0 : row.getCnt();
            Integer status = row.getStatus();
            if (status != null && status == 0) {
                pending += cnt;
            } else if (status != null && status == 1) {
                paid += cnt;
                if (row.getAmount() != null) {
                    salesAmount = salesAmount.add(row.getAmount());
                }
            } else {
                // 2 已取消 与 3 已关闭 都归入「已关闭」：对看板读者来说，
                // 「取消」与「回补完成」是同一条订单的中间态与终态，分开显示只会让人以为总数对不上
                closed += cnt;
            }
        }

        vo.setOrderPending(pending);
        vo.setOrderPaid(paid);
        vo.setOrderClosed(closed);
        vo.setOrderTotal(pending + paid + closed);
        vo.setSalesAmount(salesAmount);

        OrderStatusRow today = statsMapper.todayStats(LocalDate.now().atStartOfDay());
        if (today != null) {
            vo.setTodayOrders(today.getCnt() == null ? 0L : today.getCnt());
            vo.setTodaySalesAmount(today.getAmount() == null ? BigDecimal.ZERO : today.getAmount());
        } else {
            vo.setTodayOrders(0L);
            vo.setTodaySalesAmount(BigDecimal.ZERO);
        }

        vo.setActivities(statsMapper.activityStats(activityLimit).stream()
                .map(this::toVO)
                .toList());
        return vo;
    }

    private ActivityStatsVO toVO(ActivityStatsRow row) {
        ActivityStatsVO vo = new ActivityStatsVO();
        vo.setActivityId(row.getActivityId());
        vo.setTitle(row.getTitle());
        vo.setStatus(row.getStatus());
        ActivityStatus status = ActivityStatus.of(row.getStatus());
        vo.setStatusDesc(status == null ? "未知" : status.getDesc());
        vo.setDbStock(row.getDbStock());
        vo.setOrderCount(row.getOrderCount());
        vo.setPaidCount(row.getPaidCount());
        vo.setSalesAmount(row.getSalesAmount());
        return vo;
    }
}
