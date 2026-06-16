package com.suke.czx.modules.live.infrastructure.websocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 直播 WebSocket 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "live.websocket")
public class LiveWsProperties {

    /**
     * 消息投递模式
     * <ul>
     *   <li><b>single</b>：单实例模式，直接通过 SimpleBroker 推送（零延迟，不支持多实例）</li>
     *   <li><b>cluster</b>：集群模式，通过 Redis Pub/Sub 中继到所有实例再转发（支持多实例，延迟 1-3ms）</li>
     * </ul>
     */
    private Mode mode = Mode.single;

    public enum Mode {
        single,
        cluster
    }
}
