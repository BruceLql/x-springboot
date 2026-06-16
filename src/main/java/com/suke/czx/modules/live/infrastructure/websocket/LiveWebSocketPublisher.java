package com.suke.czx.modules.live.infrastructure.websocket;

import com.suke.czx.modules.live.interfaces.vo.LiveRoomLiveDataVO;
import com.suke.czx.modules.live.interfaces.vo.LiveTaskStatisticsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 直播 WebSocket 消息推送服务
 * <p>
 * 通过 {@link LiveWsProperties#getMode()} 控制投递模式：
 * <ul>
 *   <li><b>single</b>：直接通过 SimpleBroker 推送，零延迟，仅支持单实例部署</li>
 *   <li><b>cluster</b>：通过 Redis Pub/Sub 中继到所有实例再转发给本地 STOMP 客户端</li>
 * </ul>
 */
@Slf4j
@Component
public class LiveWebSocketPublisher {

    private final LiveWsProperties wsProperties;
    private final LiveWsRedisRelay redisRelay;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC_PREFIX = "/topic/live/";

    public LiveWebSocketPublisher(LiveWsProperties wsProperties,
                                  LiveWsRedisRelay redisRelay,
                                  SimpMessagingTemplate messagingTemplate) {
        this.wsProperties = wsProperties;
        this.redisRelay = redisRelay;
        this.messagingTemplate = messagingTemplate;
        log.info("Live WebSocket 推送模式: {}", wsProperties.getMode());
    }

    public void pushStatistics(String taskId, LiveTaskStatisticsVO statistics) {
        if (taskId == null || statistics == null) return;
        String destination = TOPIC_PREFIX + taskId + "/statistics";
        LiveWsMessage<LiveTaskStatisticsVO> msg = LiveWsMessage.of("statistics", taskId, statistics);
        send(destination, msg);
    }

    public void pushNewData(String taskId, String type, Object data) {
        if (taskId == null || type == null || data == null) return;
        String destination = TOPIC_PREFIX + taskId + "/" + type;
        LiveWsMessage<Object> msg = LiveWsMessage.of(type, taskId, data);
        send(destination, msg);
    }

    public void pushLiveData(String taskId, LiveRoomLiveDataVO liveData) {
        if (taskId == null || liveData == null) return;
        String destination = TOPIC_PREFIX + taskId + "/liveData";
        LiveWsMessage<LiveRoomLiveDataVO> msg = LiveWsMessage.of("liveData", taskId, liveData);
        send(destination, msg);
    }

    private void send(String destination, LiveWsMessage<?> msg) {
        try {
            if (wsProperties.getMode() == LiveWsProperties.Mode.cluster) {
                redisRelay.publish(destination, msg);
            } else {
                messagingTemplate.convertAndSend(destination, msg);
            }
        } catch (Exception e) {
            log.error("WebSocket 推送失败: destination={}", destination, e);
        }
    }
}
