package com.linjian.seckill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后台订单视图。
 * <p>
 * 与 C 端的 {@link OrderVO} 分开而不是加字段复用：后台要多看到 userId、
 * 所属活动这些「运营视角」信息，C 端多暴露一个 userId 都是多余的攻击面。
 */
@Data
@Schema(description = "后台订单")
public class AdminOrderVO {

    private Long id;

    private String orderNo;

    @Schema(description = "秒杀请求流水号")
    private Long requestId;

    private Long userId;

    @Schema(description = "下单用户名")
    private String username;

    private Long activityId;

    @Schema(description = "活动标题")
    private String activityTitle;

    private Long goodsId;

    private String goodsName;

    private BigDecimal price;

    private Integer quantity;

    @Schema(description = "0 待支付，1 已支付，2 已取消，3 已关闭")
    private Integer status;

    private String statusDesc;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime payTime;
}
