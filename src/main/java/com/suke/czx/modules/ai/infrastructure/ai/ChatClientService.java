package com.suke.czx.modules.ai.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatClient 封装服务
 * <p>
 * 封装 Spring AI ChatClient，提供同步/流式两种调用方式。
 * 当 Chat 未启用时，降级返回提示信息。
 */
@Slf4j
@Service
public class ChatClientService {

    @Autowired(required = false)
    private ChatClient chatClient;

    @Value("${ai.chat.system-prompt:}")
    private String defaultSystemPrompt;

    private boolean isAvailable() {
        return chatClient != null;
    }

    /**
     * 同步对话（非流式）
     *
     * @param userMessage   用户消息
     * @param systemPrompt  系统 Prompt（可为 null，使用默认）
     * @param history       历史消息列表
     * @return AI 回复文本
     */
    public String chat(String userMessage, String systemPrompt, List<Message> history) {
        if (!isAvailable()) {
            log.warn("Chat model is not available, returning fallback response");
            return "AI 服务暂未启用，请配置 Chat API Key 后重试。";
        }

        List<Message> messages = buildMessages(userMessage, systemPrompt, history);

        log.debug("Sending chat request: userMsg='{}', historySize={}", truncate(userMessage, 50), history != null ? history.size() : 0);
        ChatResponse response = chatClient.prompt(new Prompt(messages)).call().chatResponse();
        String content = response.getResult().getOutput().getText();
        log.debug("Chat response: '{}'", truncate(content, 100));
        return content;
    }

    /**
     * 流式对话（SSE）
     *
     * @param userMessage   用户消息
     * @param systemPrompt  系统 Prompt（可为 null，使用默认）
     * @param history       历史消息列表
     * @return 逐 token 流
     */
    public Flux<String> chatStream(String userMessage, String systemPrompt, List<Message> history) {
        if (!isAvailable()) {
            log.warn("Chat model is not available, returning fallback stream");
            return Flux.just("AI 服务暂未启用，请配置 Chat API Key 后重试。");
        }

        List<Message> messages = buildMessages(userMessage, systemPrompt, history);

        log.debug("Starting streaming chat: userMsg='{}'", truncate(userMessage, 50));
        return chatClient.prompt(new Prompt(messages)).stream()
                .chatResponse()
                .map(response -> response.getResult().getOutput().getText());
    }

    /**
     * 构建消息列表
     */
    private List<Message> buildMessages(String userMessage, String systemPrompt, List<Message> history) {
        List<Message> messages = new ArrayList<>();

        // 1. System Prompt
        String sysPrompt = systemPrompt != null ? systemPrompt : defaultSystemPrompt;
        if (sysPrompt != null && !sysPrompt.isBlank()) {
            messages.add(new SystemMessage(sysPrompt));
        }

        // 2. 历史消息
        if (history != null) {
            messages.addAll(history);
        }

        // 3. 当前用户消息
        messages.add(new UserMessage(userMessage));

        return messages;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
