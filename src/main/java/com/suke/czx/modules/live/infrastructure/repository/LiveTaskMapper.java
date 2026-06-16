package com.suke.czx.modules.live.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.live.domain.entity.LiveTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 直播任务Mapper接口
 */
@Mapper
public interface LiveTaskMapper extends BaseMapper<LiveTask> {
}
