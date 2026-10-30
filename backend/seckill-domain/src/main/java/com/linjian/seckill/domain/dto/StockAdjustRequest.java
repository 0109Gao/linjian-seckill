package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库存调整请求。
 * <p>
 * {@code delta} 是增量：正数加库存，负数扣库存。没有「设定为某个值」的选项，
 * 因为这套系统 Redis 与 DB 的余量都在随在途消息变化，覆盖式设定必然对不上。
 */
@Data
@Schema(description = "库存调整请求")
public class StockAdjustRequest {

    @Schema(description = "活动商品关联 ID（不是商品 ID）", example = "10001")
    @NotNull(message = "活动商品 ID 不能为空")
    private Long activityGoodsId;

    @Schema(description = "增量，正数加、负数减，不能为 0", example = "20")
    @NotNull(message = "调整数量不能为空")
    private Integer delta;

    @Schema(description = "调整原因，便于追溯", example = "运营补货")
    private String remark;
}
