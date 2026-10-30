package com.linjian.seckill.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动商品关联表，同时也是库存主体。
 * <p>
 * 为什么 version 放在这里而不是 sk_goods：秒杀的并发压力集中在「某个活动下
 * 某个商品的库存」这一个点上，版本号必须与库存行同粒度，否则多场活动会互相
 * 打断对方的乐观锁。
 */
@Data
@TableName("sk_activity_goods")
public class SkActivityGoods {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long activityId;

    private Long goodsId;

    private BigDecimal seckillPrice;

    /** 总库存。Redis 里的剩余库存以它为初值预热，DB 扣减直接减这一列 */
    private Integer stock;

    private Integer limitPerUser;

    /** 乐观锁版本号，由 OptimisticLockerInnerInterceptor 维护 */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
