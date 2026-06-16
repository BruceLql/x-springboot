package com.suke.czx.interceptor;

import com.suke.czx.common.utils.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> tokenList = accessor.getNativeHeader("token");
            if (tokenList == null || tokenList.isEmpty()) {
                log.warn("WebSocket 连接缺少 token");
                throw new IllegalArgumentException("缺少认证 token");
            }
            String token = tokenList.get(0);
            String redisKey = Constant.AUTHENTICATION_TOKEN + token;
            Object value = redisTemplate.opsForValue().get(redisKey);
            if (value == null) {
                log.warn("WebSocket 连接 token 无效: {}", token);
                throw new IllegalArgumentException("无效 token");
            }
            accessor.getSessionAttributes().put("token", token);
            log.debug("WebSocket 认证成功: token 有效");
        }
        return message;
    }
}
