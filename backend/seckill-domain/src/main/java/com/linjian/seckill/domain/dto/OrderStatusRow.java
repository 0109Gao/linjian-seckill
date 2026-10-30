package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单状态聚合行，由 {@code SkStatsMapper} 映射。
 */
@Data
@Schema(description = "订单状态聚合")
public class OrderStatusRow {

    @Schema(description = "状态码；为 null 时代表「今日汇总」这类不分状态的行")
    private Integer status;

    @Schema(description = "订单数")
    private Long cnt;

    @Schema(description = "金额")
    private BigDecimal amount;
}
