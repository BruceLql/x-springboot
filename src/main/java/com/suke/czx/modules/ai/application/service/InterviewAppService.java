package com.suke.czx.modules.ai.application.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.suke.czx.modules.ai.domain.entity.AiChatMessage;
import com.suke.czx.modules.ai.domain.entity.AiChatSession;
import com.suke.czx.modules.ai.domain.enums.DifficultyLevelEnum;
import com.suke.czx.modules.ai.domain.enums.InterviewDirectionEnum;
import com.suke.czx.modules.ai.domain.enums.SessionTypeEnum;
import com.suke.czx.modules.ai.infrastructure.ai.ChatClientService;
import com.suke.czx.modules.ai.infrastructure.ai.VectorStoreService;
import com.suke.czx.modules.ai.infrastructure.repository.AiChatMessageMapper;
import com.suke.czx.modules.ai.infrastructure.repository.AiChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试模拟应用服务
 * <p>
 * 编排面试流程：出题 → 回答评价 → 追问 → 下一题 → 生成报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAppService {

    private final ChatClientService chatClientService;
    private final VectorStoreService vectorStoreService;
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;

    @Value("${ai.interview.question-count:10}")
    private int questionCount;

    @Value("${ai.interview.max-follow-ups:3}")
    private int maxFollowUps;

    @Value("${ai.interview.pass-score:7.0}")
    private double passScore;

    // ==================== 面试 Prompt 模板 ====================

    private static final String GENERATE_QUESTION_PROMPT = """
        你是Java高级工程师面试官。请基于以下知识库内容，出一道{direction}方向的{level}难度面试题。

        要求：
        1. 题目要具体，考察候选人的深度理解，而非死记硬背
        2. {level}难度要求：初级考察基础概念，中级考察原理+实战，高级考察架构设计+源码+优化
        3. 仅返回题目文本，不要包含"面试官："等前缀

        相关知识库内容：
        {knowledge}
        """;

    private static final String EVALUATE_ANSWER_PROMPT = """
        你是Java高级工程师面试官。请从以下维度严格评价候选人的回答（每题1-10分）：

        1. 正确性（correctness）：技术要点是否准确，核心概念是否理解正确
        2. 深度（depth）：是否触及底层原理、源码实现、架构权衡
        3. 表达（expression）：逻辑是否清晰、结构化，是否有自己的思考

        当前题目：{question}
        候选人回答：{answer}
        当前难度：{level}

        请返回严格的JSON格式（不要包含其他文本）：
        {
            "correctness": 8,
            "depth": 7,
            "expression": 8,
            "total": 7.7,
            "comment": "简短评语（一句话）",
            "followUp": "追问的问题（如果总分<7必须有追问，否则为null）",
            "referenceAnswer": "参考答案要点"
        }
        """;

    private static final String GENERATE_REPORT_PROMPT = """
        你是Java高级工程师面试官。请根据候选人的面试记录生成综合评价报告。

        面试方向：{direction}
        面试记录（题目+回答+评分）：
        {records}

        请返回严格的JSON格式（不要包含其他文本）：
        {
            "totalScore": 7.5,
            "dimensions": {
                "java基础": 8.0,
                "spring生态": 7.5,
                "数据库": 6.5
            },
            "strengthTags": ["并发编程理解深入"],
            "weaknessTags": ["分布式事务理解不足"],
            "summary": "整体评价（100字以内）",
            "suggestions": "针对性学习建议（100字以内）",
            "recommendedDocs": ["推荐阅读的文档路径"]
        }

        注意：dimensions 根据实际题目涉及的知识领域动态生成。
        """;

    // ==================== 核心方法 ====================

    /**
     * 开始面试
     */
    @Transactional(transactionManager = "mysqlTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> startInterview(String userId, InterviewDirectionEnum direction) {
        // 创建面试会话
        AiChatSession session = AiChatSession.builder()
                .userId(userId)
                .title("模拟面试-" + direction.getDesc())
                .sessionType(SessionTypeEnum.INTERVIEW)
                .interviewDirection(direction)
                .difficultyLevel(DifficultyLevelEnum.JUNIOR)
                .status("ACTIVE")
                .questionIndex(1)
                .build();
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);

        // 生成第一题
        Map<String, Object> firstQuestion = generateQuestion(session);

        // 保存第一题到数据库（与 nextQuestion 保持一致）
        saveMessage(session.getId(), "ASSISTANT",
                "【第1题/" + questionCount + "】" + firstQuestion.get("question").toString(),
                null);

        return Map.of(
                "sessionId", session.getId(),
                "questionIndex", 1,
                "totalQuestions", questionCount,
                "direction", direction.getDesc(),
                "question", firstQuestion.get("question")
        );
    }

    /**
     * 提交回答
     */
    @Transactional(transactionManager = "mysqlTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> submitAnswer(String sessionId, String answer) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !"ACTIVE".equals(session.getStatus())) {
            throw new RuntimeException("面试会话不存在或已结束");
        }

        int currentIndex = session.getQuestionIndex();

        // 1. 获取当前题目
        AiChatMessage questionMsg = getLastQuestionMessage(sessionId, currentIndex);

        // 2. 保存用户回答
        saveMessage(sessionId, "USER", answer, null);

        // 3. 评价回答
        Map<String, Object> evaluation = evaluateAnswer(
                questionMsg != null ? questionMsg.getContent() : "",
                answer,
                session.getDifficultyLevel()
        );

        // 4. 保存评价
        saveMessage(sessionId, "ASSISTANT",
                "【评分】" + evaluation.get("total") + "分\n" +
                        "正确性：" + evaluation.get("correctness") + " | " +
                        "深度：" + evaluation.get("depth") + " | " +
                        "表达：" + evaluation.get("expression") + "\n" +
                        "评语：" + evaluation.get("comment") + "\n" +
                        "参考：" + evaluation.get("referenceAnswer"),
                null);

        // 5. 记录评分 metadata
        Map<String, Object> metadata = Map.of(
                "questionIndex", currentIndex,
                "scores", evaluation
        );
        saveMessage(sessionId, "SYSTEM", JSONUtil.toJsonStr(metadata), null);

        // 6. 判断是否继续
        boolean hasFollowUp = evaluation.get("followUp") != null && !evaluation.get("followUp").toString().isEmpty();
        boolean isLastQuestion = currentIndex >= questionCount;

        if (hasFollowUp) {
            // 有追问，当前题号不变
            String followUp = evaluation.get("followUp").toString();
            saveMessage(sessionId, "ASSISTANT", "【追问】" + followUp, null);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);

            return Map.of(
                    "type", "followUp",
                    "sessionId", sessionId,
                    "questionIndex", currentIndex,
                    "totalQuestions", questionCount,
                    "question", followUp,
                    "scores", evaluation
            );
        } else if (isLastQuestion) {
            // 结束面试并生成报告
            return endInterview(sessionId);
        } else {
            // 下一题
            return nextQuestion(session);
        }
    }

    /**
     * 跳过当前题
     */
    @Transactional(transactionManager = "mysqlTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> skipQuestion(String sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new RuntimeException("面试会话不存在");

        saveMessage(sessionId, "SYSTEM", "候选人跳过了第" + session.getQuestionIndex() + "题", null);

        if (session.getQuestionIndex() >= questionCount) {
            return endInterview(sessionId);
        }

        return nextQuestion(session);
    }

    /**
     * 手动结束面试
     */
    @Transactional(transactionManager = "mysqlTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> endInterview(String sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new RuntimeException("面试会话不存在");

        // 生成报告
        Map<String, Object> report = generateReport(session);

        // 更新会话
        session.setStatus("CLOSED");
        session.setTotalScore(BigDecimal.valueOf(
                ((Number) report.get("totalScore")).doubleValue()
        ).setScale(2, RoundingMode.HALF_UP));
        session.setReport(JSONUtil.toJsonStr(report));
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        saveMessage(sessionId, "SYSTEM", "面试结束，报告已生成", null);

        return Map.of(
                "type", "end",
                "sessionId", sessionId,
                "report", report
        );
    }

    /**
     * 查询面试记录列表
     */
    public List<AiChatSession> listInterviewSessions(String userId) {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .eq(AiChatSession::getSessionType, SessionTypeEnum.INTERVIEW)
                        .orderByDesc(AiChatSession::getCreateTime)
        );
    }

    /**
     * 查询面试详情
     */
    public Map<String, Object> getInterviewDetail(String sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new RuntimeException("面试会话不存在");

        List<AiChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .orderByAsc(AiChatMessage::getCreateTime)
        );

        return Map.of(
                "session", session,
                "messages", messages
        );
    }

    // ==================== 私有方法 ====================

    private Map<String, Object> nextQuestion(AiChatSession session) {
        // 动态调整难度
        adjustDifficulty(session);

        // 更新题号
        session.setQuestionIndex(session.getQuestionIndex() + 1);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        // 生成下一题
        Map<String, Object> questionData = generateQuestion(session);

        // 保存题目
        saveMessage(sessionId(session), "ASSISTANT",
                "【第" + session.getQuestionIndex() + "题/" + questionCount + "】" +
                        questionData.get("question").toString(),
                null);

        return Map.of(
                "type", "nextQuestion",
                "sessionId", session.getId(),
                "questionIndex", session.getQuestionIndex(),
                "totalQuestions", questionCount,
                "difficulty", session.getDifficultyLevel().getDesc(),
                "question", questionData.get("question")
        );
    }

    private String sessionId(AiChatSession session) {
        return session.getId();
    }

    private Map<String, Object> generateQuestion(AiChatSession session) {
        InterviewDirectionEnum direction = session.getInterviewDirection();
        DifficultyLevelEnum level = session.getDifficultyLevel();

        // 从知识库检索相关素材
        String category = direction.getKnowledgeCategory();
        List<Document> knowledgeDocs = category != null
                ? vectorStoreService.searchWithFilter(
                direction.getDesc() + " " + level.getDesc() + " 面试题",
                3,
                java.util.Map.of("category", category))
                : vectorStoreService.search(
                direction.getDesc() + " " + level.getDesc() + " 核心概念",
                3);

        StringBuilder knowledge = new StringBuilder();
        for (Document doc : knowledgeDocs) {
            knowledge.append(doc.getText()).append("\n\n");
        }

        String prompt = GENERATE_QUESTION_PROMPT
                .replace("{direction}", direction.getDesc())
                .replace("{level}", level.getDesc())
                .replace("{knowledge}", !knowledge.isEmpty() ? knowledge.toString() : "无特定参考，请根据你的专业知识出题");

        String question = chatClientService.chat(prompt, "你是严格的Java面试官，仅返回题目文本。", List.of());
        return Map.of("question", question.trim());
    }

    private Map<String, Object> evaluateAnswer(String question, String answer, DifficultyLevelEnum level) {
        String prompt = EVALUATE_ANSWER_PROMPT
                .replace("{question}", question)
                .replace("{answer}", answer)
                .replace("{level}", level.getDesc());

        String response = chatClientService.chat(
                prompt,
                "你是严格的面试评分官。请仅返回JSON格式评分结果，不要包含其他文本。",
                List.of()
        );

        try {
            // 尝试提取 JSON
            String json = extractJson(response);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = JSONUtil.toBean(json, Map.class);
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse evaluation JSON, using defaults. Response: {}", response);
            return Map.of(
                    "correctness", 5,
                    "depth", 5,
                    "expression", 5,
                    "total", 5.0,
                    "comment", "评分解析失败",
                    "followUp", "请详细解释你的理解",
                    "referenceAnswer", "请查看知识库相关章节"
            );
        }
    }

    private Map<String, Object> generateReport(AiChatSession session) {
        // 收集所有问答记录
        List<AiChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, session.getId())
                        .orderByAsc(AiChatMessage::getCreateTime)
        );

        StringBuilder records = new StringBuilder();
        for (AiChatMessage msg : messages) {
            if ("USER".equals(msg.getRole())) {
                records.append("Q: ").append(truncate(msg.getContent(), 200)).append("\n");
            } else if ("ASSISTANT".equals(msg.getRole()) && msg.getContent() != null && msg.getContent().contains("【评分】")) {
                records.append("Score: ").append(msg.getContent()).append("\n");
            }
        }

        String prompt = GENERATE_REPORT_PROMPT
                .replace("{direction}", session.getInterviewDirection() != null
                        ? session.getInterviewDirection().getDesc() : "综合")
                .replace("{records}", records.toString());

        String response = chatClientService.chat(
                prompt,
                "你是面试评估专家。请仅返回JSON格式报告。",
                List.of()
        );

        try {
            String json = extractJson(response);
            @SuppressWarnings("unchecked")
            Map<String, Object> report = JSONUtil.toBean(json, Map.class);
            return report;
        } catch (Exception e) {
            log.warn("Failed to parse report JSON. Response: {}", response);
            return Map.of(
                    "totalScore", 0.0,
                    "dimensions", new LinkedHashMap<>(),
                    "strengthTags", List.of(),
                    "weaknessTags", List.of(),
                    "summary", "报告生成失败",
                    "suggestions", "请重试",
                    "recommendedDocs", List.of()
            );
        }
    }

    private void adjustDifficulty(AiChatSession session) {
        // 根据最近的评分动态调整难度
        List<AiChatMessage> recentScores = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, session.getId())
                        .eq(AiChatMessage::getRole, "SYSTEM")
                        .orderByDesc(AiChatMessage::getCreateTime)
                        .last("LIMIT 3")
        );

        double avgScore = 5.0;
        int count = 0;
        for (AiChatMessage msg : recentScores) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = JSONUtil.toBean(msg.getContent(), Map.class);
                @SuppressWarnings("unchecked")
                Map<String, Object> scores = (Map<String, Object>) metadata.get("scores");
                if (scores != null && scores.get("total") != null) {
                    avgScore += ((Number) scores.get("total")).doubleValue();
                    count++;
                }
            } catch (Exception ignored) {
            }
        }
        if (count > 0) avgScore /= count;

        DifficultyLevelEnum newLevel;
        if (avgScore >= 8.0) {
            newLevel = DifficultyLevelEnum.SENIOR;
        } else if (avgScore >= 6.0) {
            newLevel = DifficultyLevelEnum.MIDDLE;
        } else {
            newLevel = DifficultyLevelEnum.JUNIOR;
        }

        if (newLevel != session.getDifficultyLevel()) {
            log.info("Adjusting difficulty: {} -> {} (avgScore={})",
                    session.getDifficultyLevel(), newLevel, avgScore);
            session.setDifficultyLevel(newLevel);
        }
    }

    private AiChatMessage getLastQuestionMessage(String sessionId, int currentIndex) {
        List<AiChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .eq(AiChatMessage::getRole, "ASSISTANT")
                        .like(AiChatMessage::getContent, "【第" + currentIndex + "题")
                        .orderByDesc(AiChatMessage::getCreateTime)
                        .last("LIMIT 1")
        );
        // 如果找不到（如追问情况），回退找最近的ASSISTANT消息
        if (messages.isEmpty()) {
            messages = messageMapper.selectList(
                    new LambdaQueryWrapper<AiChatMessage>()
                            .eq(AiChatMessage::getSessionId, sessionId)
                            .eq(AiChatMessage::getRole, "ASSISTANT")
                            .orderByDesc(AiChatMessage::getCreateTime)
                            .last("LIMIT 1")
            );
        }
        return messages.isEmpty() ? null : messages.get(0);
    }

    private AiChatMessage saveMessage(String sessionId, String role, String content,
                                       List<Map<String, Object>> sources) {
        AiChatMessage msg = AiChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .sources(sources != null ? JSONUtil.toJsonStr(sources) : null)
                .createTime(LocalDateTime.now())
                .build();
        messageMapper.insert(msg);
        return msg;
    }

    private String extractJson(String response) {
        // 尝试提取 JSON 块 ```json ... ``` 或直接 {...}
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
