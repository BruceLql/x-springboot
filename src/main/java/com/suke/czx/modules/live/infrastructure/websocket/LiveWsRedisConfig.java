package com.suke.czx.modules.live.infrastructure.websocket;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * WebSocket Redis Pub/Sub 中继配置
 * <p>
 * 仅在 {@code live.websocket.mode=cluster} 时生效。
 * 单实例模式下不创建 Redis 监听容器，减少不必要的 Redis 连接和订阅开销。
 */
@Configuration
@ConditionalOnProperty(name = "live.websocket.mode", havingValue = "cluster")
public class LiveWsRedisConfig {

    @Bean
    public RedisMessageListenerContainer liveWsRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            LiveWsRedisRelay relayListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(relayListener,
                new PatternTopic(LiveWsRedisRelay.REDIS_CHANNEL_PREFIX + "*"));
        return container;
    }
}
