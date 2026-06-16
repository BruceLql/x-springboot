package com.suke.czx.modules.live.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.live.domain.entity.LiveChat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 直播聊天数据Mapper接口
 */
@Mapper
public interface LiveChatMapper extends BaseMapper<LiveChat> {
}
