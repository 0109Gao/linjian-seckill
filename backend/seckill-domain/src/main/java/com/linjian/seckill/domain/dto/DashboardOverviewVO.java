package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 监控看板总览。
 */
@Data
@Schema(description = "监控看板总览")
public class DashboardOverviewVO {

    @Schema(description = "活动总数")
    private Long activityTotal;

    @Schema(description = "进行中活动数")
    private Long activityRunning;

    @Schema(description = "商品总数")
    private Long goodsTotal;

    @Schema(description = "订单总数")
    private Long orderTotal;

    @Schema(description = "待支付订单数")
    private Long orderPending;

    @Schema(description = "已支付订单数")
    private Long orderPaid;

    @Schema(description = "已关闭订单数")
    private Long orderClosed;

    @Schema(description = "累计成交额")
    private BigDecimal salesAmount;

    @Schema(description = "今日新增订单")
    private Long todayOrders;

    @Schema(description = "今日成交额")
    private BigDecimal todaySalesAmount;

    @Schema(description = "活动明细，按开始时间倒序")
    private List<ActivityStatsVO> activities;
}
