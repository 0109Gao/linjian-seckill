package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "商品新增/修改请求")
public class GoodsSaveRequest {

    @Schema(description = "商品名", example = "林间手冲咖啡豆 250g")
    @NotBlank(message = "商品名不能为空")
    @Size(max = 64, message = "商品名最长 64 字")
    private String name;

    @Schema(description = "副标题", example = "中浅烘焙 · 柑橘尾韵")
    @Size(max = 128, message = "副标题最长 128 字")
    private String subtitle;

    @Schema(description = "主图地址")
    @Size(max = 255, message = "图片地址最长 255 字")
    private String imageUrl;

    @Schema(description = "原价", example = "128.00")
    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0.01", message = "原价必须大于 0")
    private BigDecimal originPrice;

    @Schema(description = "1 上架，0 下架", example = "1")
    private Integer status;
}
