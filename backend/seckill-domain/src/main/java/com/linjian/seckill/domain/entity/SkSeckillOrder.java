package com.linjian.seckill.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单。
 * <p>
 * 三个唯一索引各承担一件事，缺一不可：
 * <ul>
 *   <li>{@code uk_request}：消费者重复消费时插入冲突，保证一个请求只落一单；</li>
 *   <li>{@code uk_user_activity_goods}：同一用户在同一活动下同一商品只能买一次，
 *       这是 Redis 用户防重被绕过时的最后一道闸门；</li>
 *   <li>{@code uk_order_no}：对外展示的订单号不能重复。</li>
 * </ul>
 * 注意这里没有逻辑删除列：订单是资金凭证，只能改状态，不允许删除。
 */
@Data
@TableName("sk_seckill_order")
public class SkSeckillOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    /** 秒杀请求流水号，前端拿它轮询下单结果 */
    private Long requestId;

    private Long userId;

    private Long activityId;

    private Long activityGoodsId;

    private Long goodsId;

    /** 商品名快照：商品改名后历史订单仍应显示下单时的名字 */
    private String goodsName;

    /** 成交价快照：活动改价不影响已生成的订单 */
    private BigDecimal price;

    private Integer quantity;

    private Integer status;

    /** 支付截止时间，关单任务按此字段扫描 */
    private LocalDateTime expireTime;

    private LocalDateTime payTime;

    private LocalDateTime cancelTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
