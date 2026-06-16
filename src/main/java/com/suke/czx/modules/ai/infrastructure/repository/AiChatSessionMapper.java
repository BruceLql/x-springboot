package com.suke.czx.modules.ai.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.ai.domain.entity.AiChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 会话 Mapper
 */
@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {
}
