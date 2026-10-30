package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "活动列表项")
public class ActivityVO {

    private Long id;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Schema(description = "0 待开始，1 进行中，2 已结束，3 已取消")
    private Integer status;

    @Schema(description = "状态中文描述，前端直接展示，避免每处都写一遍映射")
    private String statusDesc;

    @Schema(description = "该活动下的商品数量")
    private Integer goodsCount;

    private LocalDateTime createTime;
}
