package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 库存调整结果。
 * <p>
 * {@code redisSynced} 单独返回而不是吞掉：DB 改成功但 Redis 没同步上，
 * 是「管理员看到加了库存、用户抢不到」的经典故障。前端必须把这个差异显示出来。
 */
@Data
@Schema(description = "库存调整结果")
public class StockAdjustVO {

    private Long activityGoodsId;

    private Long goodsId;

    private String goodsName;

    @Schema(description = "调整前 DB 库存")
    private Integer stockBefore;

    @Schema(description = "调整后 DB 库存")
    private Integer stockAfter;

    @Schema(description = "调整后 Redis 剩余库存；null 表示该活动尚未预热")
    private Long redisRemaining;

    @Schema(description = "Redis 是否已同步成功；未预热时为 false")
    private Boolean redisSynced;

    @Schema(description = "提示信息")
    private String message;
}
