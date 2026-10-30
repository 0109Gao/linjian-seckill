package com.linjian.seckill.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品表。只描述「商品是什么」，不含任何活动与库存信息。
 * <p>
 * 取舍：库存不放这里，而放在 sk_activity_goods 上。同一个商品可以参加多场
 * 活动、各自有独立库存，把库存挂在商品上会强迫多场活动共享一个库存池，
 * 秒杀场景下这是错的。
 */
@Data
@TableName("sk_goods")
public class SkGoods {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String subtitle;

    private String imageUrl;

    /** 原价，仅用于前端展示划线价，不参与任何计算 */
    private BigDecimal originPrice;

    /** 1 上架，0 下架 */
    private Integer status;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
