package com.suke.czx.modules.live.infrastructure.websocket;

import com.suke.czx.common.utils.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis Pub/Sub 的 WebSocket 跨节点消息中继
 * <p>
 * 发布端：将消息发布到 Redis 频道，所有实例（包括自己）都会收到
 * 订阅端：收到 Redis 消息后，转发到本地 STOMP 客户端
 * <p>
 * 架构：Client <-STOMP-> SimpleBroker <-Redis Pub/Sub-> 其他实例
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveWsRedisRelay implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis 频道前缀
     */
    public static final String REDIS_CHANNEL_PREFIX = Constant.SYSTEM_NAME + "live:ws:relay:";

    /**
     * 将消息发布到 Redis Pub/Sub（所有实例都会收到并转发给本地客户端）
     */
    public void publish(String destination, LiveWsMessage<?> wsMessage) {
        try {
            redisTemplate.convertAndSend(REDIS_CHANNEL_PREFIX + destination, wsMessage);
            log.debug("Redis 发布消息: destination={}, type={}", destination, wsMessage.getType());
        } catch (Exception e) {
            log.error("Redis 发布消息失败: destination={}, type={}", destination, wsMessage.getType(), e);
        }
    }

    /**
     * 收到 Redis 消息后，转发到本地 STOMP 客户端
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            Object body = redisTemplate.getValueSerializer().deserialize(message.getBody());
            if (body instanceof LiveWsMessage<?> wsMessage) {
                String channel = new String(message.getChannel());
                // 从 Redis channel 提取 STOMP destination
                String destination = channel.substring(REDIS_CHANNEL_PREFIX.length());
                messagingTemplate.convertAndSend(destination, wsMessage);
                log.debug("Redis 转发消息至 STOMP: destination={}", destination);
            }
        } catch (Exception e) {
            log.error("Redis 消息转发 STOMP 失败", e);
        }
    }
}
