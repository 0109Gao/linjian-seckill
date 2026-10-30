package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "商品视图")
public class GoodsVO {

    private Long id;

    private String name;

    private String subtitle;

    private String imageUrl;

    private BigDecimal originPrice;

    @Schema(description = "1 上架，0 下架")
    private Integer status;

    private LocalDateTime createTime;
}
