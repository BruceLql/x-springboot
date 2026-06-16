package com.suke.czx.modules.live.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.live.domain.entity.LiveLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 直播点赞数据Mapper接口
 */
@Mapper
public interface LiveLikeMapper extends BaseMapper<LiveLike> {
}
