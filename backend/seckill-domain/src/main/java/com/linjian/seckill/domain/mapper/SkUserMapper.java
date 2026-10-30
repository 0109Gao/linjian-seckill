package com.linjian.seckill.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linjian.seckill.domain.entity.SkUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。复杂查询写在 XML 或注解中，简单单表操作由 BaseMapper 提供。
 */
@Mapper
public interface SkUserMapper extends BaseMapper<SkUser> {
}
