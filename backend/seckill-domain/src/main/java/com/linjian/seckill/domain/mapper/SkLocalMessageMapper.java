package com.linjian.seckill.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linjian.seckill.domain.entity.SkLocalMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SkLocalMessageMapper extends BaseMapper<SkLocalMessage> {

    /**
     * 扫描待重投的消息。
     * <p>
     * 条件里的 {@code next_retry_time IS NULL OR <= now} 让退避生效：
     * 第一次失败后不立刻重试，而是等一段时间，避免 Broker 短暂抖动时的
     * 空转把 CPU 打满。
     */
    @Select("""
            SELECT *
            FROM sk_local_message
            WHERE status IN (0, 2)
              AND (next_retry_time IS NULL OR next_retry_time <= #{now})
            ORDER BY id
            LIMIT #{limit}
            """)
    List<SkLocalMessage> selectPending(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
