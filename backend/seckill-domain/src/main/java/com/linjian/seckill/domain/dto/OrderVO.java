package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "秒杀订单")
public class OrderVO {

    @Schema(description = "订单 ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "商品名（下单时快照）")
    private String goodsName;

    @Schema(description = "成交价")
    private BigDecimal price;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "状态：0 待支付，1 已支付，2 已取消，3 已关闭")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "支付截止时间")
    private LocalDateTime expireTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;
}
