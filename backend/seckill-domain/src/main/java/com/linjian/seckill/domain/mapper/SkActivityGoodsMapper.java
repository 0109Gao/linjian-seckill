package com.linjian.seckill.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linjian.seckill.domain.entity.SkActivityGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SkActivityGoodsMapper extends BaseMapper<SkActivityGoods> {

    /**
     * 库存扣减。
     * <p>
     * <b>刻意不带 {@code version} 条件</b>。这条 SQL 本身就是原子的：
     * {@code SET stock = stock - n WHERE stock >= n} 由 InnoDB 的行锁保证
     * 「判断 + 扣减」不可分割，20 个并发同时扣减会排队执行，每个都能成功，
     * 不存在 CAS 失败。
     * <p>
     * 早期版本带了 {@code AND version = #{version}}，压测里立刻暴露出问题：
     * 单行热点下 20 个消费者读到的都是同一个版本号，只有 1 个能更新成功，
     * 其余 19 个全部失败，重试 3 次仍然抢不到，只能把消息重新入队。
     * 乐观锁适合「读多写少、冲突后可重试」的场景，不适合秒杀这种
     * 「单行极高并发写」的场景——在这里它就是一把没必要的自旋锁。
     * <p>
     * version 列仍然自增，作为「这一行被改过多少次」的审计信息保留，
     * 但不再参与并发控制。
     *
     * @return 影响行数，1 成功，0 表示库存不足
     */
    @Update("""
            UPDATE sk_activity_goods
               SET stock   = stock - #{quantity},
                   version = version + 1
             WHERE id = #{id}
               AND stock >= #{quantity}
            """)
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 回补库存（关单、下单失败时调用）。
     * <p>
     * 回补不带乐观锁版本条件：这里做的是加法，两笔回补同时执行也不会丢失更新，
     * 带版本号反而会让第二笔回补莫名失败，导致库存永久少掉。
     */
    @Update("""
            UPDATE sk_activity_goods
               SET stock   = stock + #{quantity},
                   version = version + 1
             WHERE id = #{id}
            """)
    int restoreStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 运营侧人工调整库存，正数加、负数减。
     * <p>
     * 语义是「增量」而不是「设定值」：这套系统的扣减是预扣即扣，Redis 与 DB
     * 的余量都在变动。若按设定值覆盖，管理员填 100 时在途消息还没落库，
     * 落库后再扣一次就会比 100 少，两边对不上。增量调整天然与在途无关。
     * <p>
     * {@code stock + delta >= 0} 把「不能减成负库存」交给数据库判断，
     * 避免先查后改的竞态。
     *
     * @return 影响行数，1 成功，0 表示调整后会变成负库存或记录不存在
     */
    @Update("""
            UPDATE sk_activity_goods
               SET stock   = stock + #{delta},
                   version = version + 1
             WHERE id = #{id}
               AND stock + #{delta} >= 0
            """)
    int adjustStock(@Param("id") Long id, @Param("delta") Integer delta);
}
