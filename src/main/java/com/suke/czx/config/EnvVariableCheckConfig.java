package com.suke.czx.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 环境变量加载验证配置
 * <p>
 * 启动时检查 AI 相关环境变量是否已设置，日志中仅显示脱敏后的值。
 */
@Slf4j
@Configuration
public class EnvVariableCheckConfig {

    @Value("${AI_CHAT_API_KEY:NOT_SET}")
    private String chatApiKey;

    @Value("${AI_CHAT_BASE_URL:NOT_SET}")
    private String chatBaseUrl;

    @Value("${AI_CHAT_MODEL:NOT_SET}")
    private String chatModel;

    @Value("${AI_EMBEDDING_API_KEY:NOT_SET}")
    private String embeddingApiKey;

    @Value("${AI_EMBEDDING_BASE_URL:NOT_SET}")
    private String embeddingBaseUrl;

    @Value("${AI_EMBEDDING_MODEL:NOT_SET}")
    private String embeddingModel;

    @PostConstruct
    public void verifyEnvVariables() {
        log.info("=== AI 环境变量加载验证 ===");
        log.info("AI_CHAT_API_KEY: {}", mask(chatApiKey));
        log.info("AI_CHAT_BASE_URL: {}", chatBaseUrl);
        log.info("AI_CHAT_MODEL: {}", chatModel);
        log.info("AI_EMBEDDING_API_KEY: {}", mask(embeddingApiKey));
        log.info("AI_EMBEDDING_BASE_URL: {}", embeddingBaseUrl);
        log.info("AI_EMBEDDING_MODEL: {}", embeddingModel);

        if ("NOT_SET".equals(chatApiKey)) {
            log.warn("⚠️ AI_CHAT_API_KEY 未设置！");
        }
        if ("NOT_SET".equals(embeddingApiKey)) {
            log.warn("⚠️ AI_EMBEDDING_API_KEY 未设置！");
        }
    }

    /**
     * 对敏感字段进行脱敏处理，仅显示前4位和后4位。
     */
    private static String mask(String value) {
        if (value == null || "NOT_SET".equals(value) || value.length() <= 8) {
            return value;
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
