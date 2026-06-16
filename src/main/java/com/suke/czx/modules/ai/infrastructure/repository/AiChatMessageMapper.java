package com.suke.czx.modules.ai.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.ai.domain.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话消息 Mapper
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
}
