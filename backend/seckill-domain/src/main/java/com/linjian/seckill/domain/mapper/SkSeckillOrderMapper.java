package com.linjian.seckill.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linjian.seckill.domain.entity.SkSeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SkSeckillOrderMapper extends BaseMapper<SkSeckillOrder> {

    /**
     * 统计某活动某商品的有效占用数量（待支付 + 已支付）。
     * <p>
     * 对账与 DB 库存回补都依赖这个口径：已取消且未回补的订单不占库存。
     */
    @Select("""
            SELECT COALESCE(SUM(quantity), 0)
            FROM sk_seckill_order
            WHERE activity_id = #{activityId}
              AND goods_id = #{goodsId}
              AND status IN (0, 1)
            """)
    int sumActiveQuantity(@Param("activityId") Long activityId, @Param("goodsId") Long goodsId);

    /**
     * 查出到点未支付的订单主键。只取 ID，避免长事务里带着整行数据。
     */
    @Select("""
            SELECT id
            FROM sk_seckill_order
            WHERE status = 0
              AND expire_time <= #{now}
            ORDER BY expire_time
            LIMIT #{limit}
            """)
    List<Long> selectTimeoutIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * 抢占式关单：只有把状态从 0 改成 2 的那次调用返回 1，
     * 并发的另一次调用返回 0。这样回补库存不会重复执行。
     */
    @Update("""
            UPDATE sk_seckill_order
            SET status = 2, cancel_time = #{now}
            WHERE id = #{id} AND status = 0
            """)
    int cancelIfPending(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 回补完成后把状态推进到「已关闭」。
     */
    @Update("""
            UPDATE sk_seckill_order
            SET status = 3
            WHERE id = #{id} AND status = 2
            """)
    int markRestored(@Param("id") Long id);

    /**
     * 支付成功。同样是条件更新，防止「关单与支付并发」时把已支付的订单改成已关闭。
     */
    @Update("""
            UPDATE sk_seckill_order
            SET status = 1, pay_time = #{payTime}
            WHERE id = #{id} AND status = 0
            """)
    int payIfPending(@Param("id") Long id, @Param("payTime") LocalDateTime payTime);
}
