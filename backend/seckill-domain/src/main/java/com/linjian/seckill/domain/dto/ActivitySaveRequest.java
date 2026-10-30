package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动新增请求。
 * <p>
 * 时间格式固定 "yyyy-MM-dd HH:mm:ss"，与全局 Jackson 配置一致；
 * 传 ISO 带 T 的格式会反序列化失败。
 */
@Data
@Schema(description = "活动新增请求")
public class ActivitySaveRequest {

    @Schema(description = "活动标题", example = "周五晚八点 · 咖啡豆专场")
    @NotBlank(message = "活动标题不能为空")
    @Size(max = 64, message = "活动标题最长 64 字")
    private String title;

    @Schema(description = "活动说明")
    @Size(max = 255, message = "活动说明最长 255 字")
    private String description;

    @Schema(description = "开始时间", example = "2026-10-31 20:00:00")
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-10-31 22:00:00")
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "活动商品列表，至少一个")
    @NotEmpty(message = "活动至少需要一个商品")
    @Valid
    private List<ActivityGoodsItemRequest> items;
}
