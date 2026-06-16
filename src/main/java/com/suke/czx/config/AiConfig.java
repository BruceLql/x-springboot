package com.suke.czx.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring AI 配置
 * <p>
 * 支持独立配置 Chat 和 Embedding 的 base-url/api-key/model，
 * 实现 Provider 自由切换（DeepSeek/ZhipuAI/Moonshot/SiliconFlow 等）
 */
@Slf4j
@Configuration
public class AiConfig {

    // ==================== Chat 配置 ====================

    @Value("${spring.ai.openai.api-key:}")
    private String chatApiKey;

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String chatBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String chatModel;

    // ==================== Embedding 配置 ====================

    @Value("${spring.ai.openai.embedding.api-key:}")
    private String embeddingApiKey;

    @Value("${spring.ai.openai.embedding.base-url:https://api.siliconflow.cn}")
    private String embeddingBaseUrl;

    @Value("${spring.ai.openai.embedding.options.model:BAAI/bge-m3}")
    private String embeddingModel;

    // ==================== Chat Beans ====================

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai.chat", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OpenAiApi chatOpenAiApi() {
        if (chatApiKey == null || chatApiKey.isBlank()) {
            throw new IllegalStateException(
                    "Chat API Key 未配置！请设置环境变量 AI_CHAT_API_KEY");
        }
        log.info("AI Chat Provider: baseUrl={}, model={}", chatBaseUrl, chatModel);
        return OpenAiApi.builder()
                .baseUrl(chatBaseUrl)
                .apiKey(chatApiKey)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai.chat", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OpenAiChatModel openAiChatModel(@Qualifier("chatOpenAiApi") OpenAiApi chatOpenAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(chatOpenAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModel)
                        .temperature(0.7)
                        .maxTokens(4096)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai.chat", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    // ==================== Embedding Beans ====================

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai.embedding", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OpenAiApi embeddingOpenAiApi() {
        if (embeddingApiKey == null || embeddingApiKey.isBlank()) {
            throw new IllegalStateException(
                "Embedding API Key 未配置！请设置环境变量 AI_EMBEDDING_API_KEY 或 AI_CHAT_API_KEY");
        }
        log.info("AI Embedding Provider: baseUrl={}, model={}", embeddingBaseUrl, embeddingModel);
        return OpenAiApi.builder()
                .baseUrl(embeddingBaseUrl)
                .apiKey(embeddingApiKey)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai.embedding", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OpenAiEmbeddingModel openAiEmbeddingModel(
            @Qualifier("embeddingOpenAiApi") OpenAiApi embeddingOpenAiApi) {
        return new OpenAiEmbeddingModel(
                embeddingOpenAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(embeddingModel)
                        .build()
        );
    }

    // ==================== VectorStore ====================

    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource.pg", name = "enabled", havingValue = "true")
    public VectorStore vectorStore(
            @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbcTemplate,
            EmbeddingModel embeddingModel) {
        log.info("Initializing PGVector store...");
        return PgVectorStore.builder(pgJdbcTemplate, embeddingModel)
                .dimensions(1024)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .build();
    }
}