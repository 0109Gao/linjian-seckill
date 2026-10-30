package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "活动详情")
public class ActivityDetailVO {

    private Long id;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Schema(description = "0 待开始，1 进行中，2 已结束，3 已取消")
    private Integer status;

    private String statusDesc;

    @Schema(description = "商品明细，含 Redis 剩余库存")
    private List<ActivityGoodsVO> items;
}
