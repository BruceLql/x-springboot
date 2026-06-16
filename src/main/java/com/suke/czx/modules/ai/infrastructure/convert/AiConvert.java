package com.suke.czx.modules.ai.infrastructure.convert;

import com.suke.czx.modules.ai.domain.entity.AiChatMessage;
import com.suke.czx.modules.ai.domain.entity.AiChatSession;
import com.suke.czx.modules.ai.domain.entity.AiKnowledge;
import com.suke.czx.modules.ai.interfaces.vo.ChatMessageVO;
import com.suke.czx.modules.ai.interfaces.vo.ChatSessionVO;
import com.suke.czx.modules.ai.interfaces.vo.KnowledgeVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * AI 模块 MapStruct 转换器
 */
@Mapper(componentModel = "spring")
public interface AiConvert {

    AiConvert INSTANCE = Mappers.getMapper(AiConvert.class);

    // Knowledge
    KnowledgeVO toVO(AiKnowledge entity);
    List<KnowledgeVO> toKnowledgeVOList(List<AiKnowledge> entities);

    // ChatSession
    @Mapping(target = "sessionType", expression = "java(entity.getSessionType() != null ? entity.getSessionType().getValue() : null)")
    @Mapping(target = "interviewDirection", expression = "java(entity.getInterviewDirection() != null ? entity.getInterviewDirection().getValue() : null)")
    @Mapping(target = "difficultyLevel", expression = "java(entity.getDifficultyLevel() != null ? entity.getDifficultyLevel().getValue() : null)")
    ChatSessionVO toVO(AiChatSession entity);
    List<ChatSessionVO> toChatSessionVOList(List<AiChatSession> entities);

    // ChatMessage
    ChatMessageVO toVO(AiChatMessage entity);
    List<ChatMessageVO> toChatMessageVOList(List<AiChatMessage> entities);
}
