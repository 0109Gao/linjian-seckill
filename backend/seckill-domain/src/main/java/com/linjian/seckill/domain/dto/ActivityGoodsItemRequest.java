package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "活动中的单个商品")
public class ActivityGoodsItemRequest {

    @Schema(description = "商品 ID（须已存在）")
    @NotNull(message = "商品 ID 不能为空")
    private Long goodsId;

    @Schema(description = "秒杀价", example = "39.90")
    @NotNull(message = "秒杀价不能为空")
    @DecimalMin(value = "0.01", message = "秒杀价必须大于 0")
    private BigDecimal seckillPrice;

    @Schema(description = "总库存", example = "100")
    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存至少为 1")
    private Integer stock;

    @Schema(description = "每人限购", example = "1")
    @Min(value = 1, message = "限购至少为 1")
    private Integer limitPerUser;
}
