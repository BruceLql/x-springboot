package com.suke.czx.modules.ai.application.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.suke.czx.modules.ai.domain.entity.AiChatMessage;
import com.suke.czx.modules.ai.domain.entity.AiChatSession;
import com.suke.czx.modules.ai.domain.enums.SessionTypeEnum;
import com.suke.czx.modules.ai.infrastructure.ai.ChatClientService;
import com.suke.czx.modules.ai.infrastructure.ai.VectorStoreService;
import com.suke.czx.modules.ai.infrastructure.repository.AiChatMessageMapper;
import com.suke.czx.modules.ai.infrastructure.repository.AiChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 对话应用服务
 * <p>
 * 编排 RAG 问答流程：检索 → 组装 Prompt → LLM 生成 → 保存消息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAppService {

    private final ChatClientService chatClientService;
    private final VectorStoreService vectorStoreService;
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;

    @Autowired
    @Qualifier("mysqlTransactionManager")
    private PlatformTransactionManager mysqlTransactionManager;

    @Value("${ai.chat.context-rounds:10}")
    private int contextRounds;

    @Value("${ai.chat.system-prompt}")
    private String defaultSystemPrompt;

    // ==================== 会话管理 ====================

    /**
     * 创建新会话
     */
    public AiChatSession createSession(String userId, String title) {
        AiChatSession session = AiChatSession.builder()
                .userId(userId)
                .title(title != null ? title : "新对话")
                .sessionType(SessionTypeEnum.CHAT)
                .status("ACTIVE")
                .questionIndex(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        sessionMapper.insert(session);
        return session;
    }

    /**
     * 获取会话列表
     */
    public List<AiChatSession> listSessions(String userId) {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .eq(AiChatSession::getSessionType, SessionTypeEnum.CHAT)
                        .eq(AiChatSession::getStatus, "ACTIVE")
                        .orderByDesc(AiChatSession::getUpdateTime)
        );
    }

    /**
     * 删除会话
     */
    public void deleteSession(String sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setStatus("CLOSED");
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    /**
     * 获取会话的历史消息
     */
    public List<AiChatMessage> getMessages(String sessionId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .orderByAsc(AiChatMessage::getCreateTime)
        );
    }

    // ==================== RAG 问答 ====================

    /**
     * 非流式 RAG 问答
     *
     * @return {messageId, answer, sources}
     */
    @Transactional(transactionManager = "mysqlTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> send(String sessionId, String userId, String content) {
        // 1. 保存用户消息
        saveMessage(sessionId, "USER", content, null);

        // 2. 更新会话标题（使用首条消息的前30字）
        updateSessionTitle(sessionId);

        // 3. 向量检索
        List<Document> retrieved = vectorStoreService.search(content, 5);

        // 4. 组装 System Prompt + 历史
        String systemPrompt = buildRagSystemPrompt(retrieved);
        List<Message> history = buildHistory(sessionId);

        // 5. 调用 LLM
        String answer = chatClientService.chat(content, systemPrompt, history);

        // 6. 构建引用来源
        List<Map<String, Object>> sources = retrieved.stream()
                .map(doc -> {
                    var meta = doc.getMetadata();
                    double distance = ((Number) meta.getOrDefault("distance", 1.0)).doubleValue();
                    double score = Math.max(0.0, 1.0 - distance);
                    return (Map<String, Object>) Map.of(
                            "title", meta.getOrDefault("title", ""),
                            "filePath", meta.getOrDefault("filePath", ""),
                            "score", score
                    );
                })
                .collect(Collectors.toList());

        // 7. 保存 AI 回复
        AiChatMessage aiMsg = saveMessage(sessionId, "ASSISTANT", answer, sources);

        // 8. 更新会话时间
        updateSessionTime(sessionId);

        return Map.of(
                "messageId", aiMsg.getId(),
                "answer", answer,
                "sources", sources
        );
    }

    /**
     * SSE 流式 RAG 问答
     * <p>
     * 注意：不能使用 @Transactional，因为响应式 Flux 是异步执行的，
     * 事务在方法返回时就提交了，Flux 生命周期内的 DB 操作需要手动管理事务。
     */
    public Flux<String> sendStream(String sessionId, String userId, String content) {
        TransactionTemplate tx = new TransactionTemplate(mysqlTransactionManager);

        // 1. 保存用户消息（显式事务）
        tx.executeWithoutResult(status -> saveMessage(sessionId, "USER", content, null));

        // 2. 向量检索
        List<Document> retrieved = vectorStoreService.search(content, 5);

        // 3. 组装 Prompt
        String systemPrompt = buildRagSystemPrompt(retrieved);
        List<Message> history = tx.execute(status -> buildHistory(sessionId));

        // 4. 流式调用 LLM
        StringBuilder fullAnswer = new StringBuilder();
        return chatClientService.chatStream(content, systemPrompt, history)
                .timeout(Duration.ofSeconds(120))
                .doOnNext(fullAnswer::append)
                .doOnError(error -> log.error("SSE stream error for session {}: {}", sessionId, error.getMessage()))
                .onErrorResume(error -> {
                    // 确保流总是能终止，前端不会一直等待
                    log.warn("SSE stream terminated with error for session {}, returning fallback", sessionId);
                    return Mono.just("\n\n[AI 服务暂时不可用，请稍后重试]");
                })
                .doOnComplete(() -> {
                    // 流结束后保存完整回复（显式事务）
                    List<Map<String, Object>> sources = retrieved.stream()
                            .map(doc -> {
                                var meta = doc.getMetadata();
                                double distance = ((Number) meta.getOrDefault("distance", 1.0)).doubleValue();
                                double score = Math.max(0.0, 1.0 - distance);
                                return (Map<String, Object>) Map.of(
                                        "title", meta.getOrDefault("title", ""),
                                        "filePath", meta.getOrDefault("filePath", ""),
                                        "score", score
                                );
                            })
                            .collect(Collectors.toList());
                    tx.executeWithoutResult(status -> {
                        saveMessage(sessionId, "ASSISTANT", fullAnswer.toString(), sources);
                        updateSessionTitle(sessionId);
                        updateSessionTime(sessionId);
                    });
                });
    }

    // ==================== 私有方法 ====================

    private AiChatMessage saveMessage(String sessionId, String role, String content,
                                       List<Map<String, Object>> sources) {
        AiChatMessage message = AiChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .sources(sources != null ? JSONUtil.toJsonStr(sources) : null)
                .tokenCount(content != null ? content.length() / 4 : 0)
                .createTime(LocalDateTime.now())
                .build();
        messageMapper.insert(message);
        return message;
    }

    private void updateSessionTitle(String sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session != null && ("新对话".equals(session.getTitle()) || session.getTitle() == null)) {
            // 取首条用户消息的前30字作为标题
            List<AiChatMessage> messages = messageMapper.selectList(
                    new LambdaQueryWrapper<AiChatMessage>()
                            .eq(AiChatMessage::getSessionId, sessionId)
                            .eq(AiChatMessage::getRole, "USER")
                            .orderByAsc(AiChatMessage::getCreateTime)
                            .last("LIMIT 1")
            );
            if (!messages.isEmpty()) {
                String content = messages.get(0).getContent();
                String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
                session.setTitle(title);
                session.setUpdateTime(LocalDateTime.now());
                sessionMapper.updateById(session);
            }
        }
    }

    private void updateSessionTime(String sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    private String buildRagSystemPrompt(List<Document> documents) {
        StringBuilder chunks = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            var doc = documents.get(i);
            var meta = doc.getMetadata();
            chunks.append("【").append(i + 1).append("】")
                    .append(meta.getOrDefault("title", ""))
                    .append(" (").append(meta.getOrDefault("filePath", "")).append(")\n")
                    .append(doc.getText())
                    .append("\n\n");
        }
        return defaultSystemPrompt.replace("{retrievedChunks}", chunks.toString());
    }

    private List<Message> buildHistory(String sessionId) {
        List<AiChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .in(AiChatMessage::getRole, "USER", "ASSISTANT")
                        .orderByDesc(AiChatMessage::getCreateTime)
                        .last("LIMIT " + (contextRounds * 2)) // N轮 = 2N 条消息
        );

        List<Message> history = new ArrayList<>();
        // 反转回时间顺序
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatMessage msg = messages.get(i);
            if ("USER".equals(msg.getRole())) {
                history.add(new UserMessage(msg.getContent()));
            } else if ("ASSISTANT".equals(msg.getRole())) {
                history.add(new AssistantMessage(msg.getContent()));
            }
        }
        return history;
    }
}
