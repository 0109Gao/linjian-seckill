package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "活动商品视图")
public class ActivityGoodsVO {

    @Schema(description = "关联记录 ID，下单时以它为准")
    private Long activityGoodsId;

    private Long goodsId;

    private String name;

    private String subtitle;

    private String imageUrl;

    private BigDecimal originPrice;

    private BigDecimal seckillPrice;

    @Schema(description = "总库存")
    private Integer stock;

    @Schema(description = "Redis 中的剩余库存；null 表示尚未预热")
    private Long remaining;

    @Schema(description = "每人限购")
    private Integer limitPerUser;
}
