package com.suke.czx.modules.live.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.live.domain.entity.LiveUserEnter;
import org.apache.ibatis.annotations.Mapper;

/**
 * 直播用户进入数据Mapper接口
 */
@Mapper
public interface LiveUserEnterMapper extends BaseMapper<LiveUserEnter> {
}
