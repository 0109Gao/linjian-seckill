package com.linjian.seckill.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消费幂等记录。
 * <p>
 * 插入成功才允许处理业务，插入冲突说明这条消息已经处理过。
 * 与「订单表的 uk_request」是两层保险：幂等表挡住重复消费，唯一索引挡住
 * 幂等表被清理或写入失败时的并发插入。
 */
@Data
@TableName("sk_idempotent_record")
public class SkIdempotentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private String bizType;

    private String bizId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
