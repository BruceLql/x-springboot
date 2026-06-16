package com.suke.czx.modules.ai.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.ai.domain.entity.AiKnowledge;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识文档 Mapper
 */
@Mapper
public interface AiKnowledgeMapper extends BaseMapper<AiKnowledge> {
}
