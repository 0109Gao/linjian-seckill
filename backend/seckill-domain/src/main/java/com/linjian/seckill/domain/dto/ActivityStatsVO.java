package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理端活动统计视图。
 */
@Data
@Schema(description = "管理端活动统计")
public class ActivityStatsVO {

    private Long activityId;

    private String title;

    private Integer status;

    private String statusDesc;

    @Schema(description = "DB 剩余总库存")
    private Long dbStock;

    @Schema(description = "订单总数")
    private Long orderCount;

    @Schema(description = "已支付订单数")
    private Long paidCount;

    @Schema(description = "成交额")
    private BigDecimal salesAmount;
}
