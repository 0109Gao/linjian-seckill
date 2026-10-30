package com.linjian.seckill.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linjian.seckill.domain.entity.SkIdempotentRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SkIdempotentRecordMapper extends BaseMapper<SkIdempotentRecord> {
}
