package com.linjian.seckill.domain.mapper;

import com.linjian.seckill.domain.dto.ActivityStatsRow;
import com.linjian.seckill.domain.dto.OrderStatusRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端统计查询。
 * <p>
 * 单独一个 Mapper 而不是塞进 SkSeckillOrderMapper：这里返回的不是实体，
 * 是跨表聚合后的读模型。混在实体 Mapper 里，后续换 ORM 或拆分读写库时
 * 会分不清哪些方法是「实体操作」哪些是「报表查询」。
 * <p>
 * 聚合全部下推到数据库：活动维度一次 JOIN 出结果，不在 Java 里循环查，
 * 避免 N+1。演示数据量小看不出差别，但这是写给人看的口径问题，
 * 不是性能调优。
 */
@Mapper
public interface SkStatsMapper {

    /**
     * 活动维度汇总：库存总量、订单数、已支付数、成交额。
     * <p>
     * 两个子查询各自聚合后再 JOIN，而不是直接 JOIN 三张表再 GROUP BY：
     * 后者会让订单行与商品行做笛卡尔积，SUM(stock) 被放大成「库存 × 订单数」。
     */
    @Select("""
            SELECT a.id                                        AS activityId,
                   a.title                                     AS title,
                   a.status                                    AS status,
                   COALESCE(g.db_stock, 0)                     AS dbStock,
                   COALESCE(o.order_count, 0)                  AS orderCount,
                   COALESCE(o.paid_count, 0)                   AS paidCount,
                   COALESCE(o.sales_amount, 0)                 AS salesAmount
              FROM sk_activity a
              LEFT JOIN (SELECT activity_id, SUM(stock) AS db_stock
                           FROM sk_activity_goods
                          GROUP BY activity_id) g
                     ON g.activity_id = a.id
              LEFT JOIN (SELECT activity_id,
                                COUNT(*)                                                  AS order_count,
                                SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END)               AS paid_count,
                                SUM(CASE WHEN status = 1 THEN price * quantity ELSE 0 END) AS sales_amount
                           FROM sk_seckill_order
                          GROUP BY activity_id) o
                     ON o.activity_id = a.id
             ORDER BY a.start_time DESC
             LIMIT #{limit}
            """)
    List<ActivityStatsRow> activityStats(@Param("limit") int limit);

    /**
     * 订单按状态汇总，用于看板的四张数字卡。
     */
    @Select("""
            SELECT status                                        AS status,
                   COUNT(*)                                      AS cnt,
                   COALESCE(SUM(price * quantity), 0)            AS amount
              FROM sk_seckill_order
             GROUP BY status
            """)
    List<OrderStatusRow> orderStats();

    /**
     * 今日新增订单与今日成交额。
     */
    @Select("""
            SELECT COUNT(*)                                                          AS cnt,
                   COALESCE(SUM(CASE WHEN status = 1 THEN price * quantity ELSE 0 END), 0) AS amount
              FROM sk_seckill_order
             WHERE create_time >= #{start}
            """)
    OrderStatusRow todayStats(@Param("start") LocalDateTime start);
}
