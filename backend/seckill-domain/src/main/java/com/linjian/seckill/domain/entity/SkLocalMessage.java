package com.linjian.seckill.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表。
 * <p>
 * 解决的是「Redis 已扣库存，但 MQ 投递失败」这个窗口：消息先与业务在同一次
 * 数据库写入里落盘，再由定时任务扫描未投递的记录重发。这样即使 Broker 在
 * 那一瞬间不可用，扣掉的库存也不会永久悬空。
 */
@Data
@TableName("sk_local_message")
public class SkLocalMessage {

    /** 待投递 */
    public static final int STATUS_PENDING = 0;

    /** 已投递 */
    public static final int STATUS_SENT = 1;

    /** 投递失败，等待重试 */
    public static final int STATUS_FAILED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private String exchangeName;

    private String routingKey;

    private String payload;

    private Integer status;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
