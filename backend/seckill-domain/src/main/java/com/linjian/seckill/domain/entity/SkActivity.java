package com.linjian.seckill.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀活动（专场）。一个活动挂多个商品，库存落在 sk_activity_goods 上。
 * <p>
 * status 是「存储态」而不是「计算态」：虽然可以由当前时间与 start/end 推导，
 * 但仍然落库，原因是预热任务需要按 status 建索引扫描待开始的活动，
 * 纯计算的写法会让扫描条件变成 start_time 范围查询，活动量大了以后
 * 索引命中率明显更差。
 */
@Data
@TableName("sk_activity")
public class SkActivity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 0 待开始，1 进行中，2 已结束，3 已取消，见 ActivityStatus */
    private Integer status;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
