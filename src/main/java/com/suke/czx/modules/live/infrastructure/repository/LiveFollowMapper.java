package com.suke.czx.modules.live.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.live.domain.entity.LiveFollow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 直播关注数据Mapper接口
 */
@Mapper
public interface LiveFollowMapper extends BaseMapper<LiveFollow> {
}
