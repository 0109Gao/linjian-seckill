package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 活动维度统计行，由 {@code SkStatsMapper.activityStats} 直接映射。
 */
@Data
@Schema(description = "活动统计")
public class ActivityStatsRow {

    private Long activityId;

    private String title;

    private Integer status;

    @Schema(description = "DB 中剩余总库存（各商品之和）")
    private Long dbStock;

    @Schema(description = "订单总数（含待支付、已支付、已关闭）")
    private Long orderCount;

    @Schema(description = "已支付订单数")
    private Long paidCount;

    @Schema(description = "已支付成交额")
    private BigDecimal salesAmount;
}
