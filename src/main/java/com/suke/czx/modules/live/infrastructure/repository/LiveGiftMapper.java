package com.suke.czx.modules.live.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.live.domain.entity.LiveGift;
import org.apache.ibatis.annotations.Mapper;

/**
 * 直播礼物数据Mapper接口
 */
@Mapper
public interface LiveGiftMapper extends BaseMapper<LiveGift> {
}
