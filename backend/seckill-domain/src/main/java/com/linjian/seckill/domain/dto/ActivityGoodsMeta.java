package com.linjian.seckill.domain.dto;

import lombok.Data;

/**
 * 预热到 Redis 的活动商品元信息。
 * <p>
 * 存在的唯一理由：让秒杀接口在扣减前不需要查 MySQL。活动状态、价格、
 * 限购数量、结束时间这些「下单必需但几乎不变」的数据，在预热时一次性
 * 写进 db1，接口里只做 Redis 读 + Lua 扣减，整条同步链路不碰数据库。
 */
@Data
public class ActivityGoodsMeta {

    private Long activityGoodsId;

    private Long activityId;

    private Long goodsId;

    private String goodsName;

    /** 秒杀价，使用字符串传输避免 JSON 反序列化丢精度 */
    private String price;

    private Integer limitPerUser;

    /** 活动开始时间（epoch 毫秒） */
    private Long startTime;

    /** 活动结束时间（epoch 毫秒） */
    private Long endTime;
}
