# WebSocket Redis Pub/Sub 跨节点消息中继方案

## 1. 背景与问题

### 1.1 原有架构

直播模块使用 STOMP over WebSocket 向客户端实时推送监控数据（统计数据、聊天、礼物、在线人数等）。原有架构如下：

```
┌──────────────┐                        ┌──────────────────┐
│  浏览器客户端  │◄── STOMP/WebSocket ──►│  实例A  SimpleBroker │
└──────────────┘                        │  (内存消息代理)      │
                                        │  ┌──────────────┐ │
┌──────────────┐                        │  │  Watcher线程  │ │
│  浏览器客户端  │◄── STOMP/WebSocket ──►│  │  (直播间连接)  │ │
└──────────────┘                        │  └──────┬───────┘ │
                                        │         │推送      │
                                        │  ┌──────▼───────┐ │
                                        │  │SimpleBroker  │ │
                                        │  │(内存Topic)    │ │
                                        │  └──────────────┘ │
                                        └──────────────────┘
```

### 1.2 核心问题

`enableSimpleBroker("/topic")` 使用的是**内存级消息代理**：

- 实例 A 上的 Watcher 推送的消息，**只有连接到实例 A 的客户端能收到**
- 连接到实例 B 的客户端**完全收不到**消息
- 实例 A 宕机后，所有 WebSocket 连接断开，消息全部丢失

这在多实例分布式部署下是致命缺陷。

## 2. 解决方案概述

### 2.1 新架构

引入 **Redis Pub/Sub 作为消息中继层**，实现跨节点广播：

```
                          Redis Pub/Sub
                     ┌──────────────────────┐
                     │  Channel:             │
                     │  x-springboot:live:   │
                     │  ws:relay:/topic/     │
                     │  live/{taskId}/*      │
                     └──┬───────┬───────┬────┘
                        │Publish │       │Publish
                        ▼        │       ▼
              ┌──────────────┐  │  ┌──────────────┐
              │   实例 A      │  │  │   实例 B      │
              │ ┌──────────┐ │  │  │ ┌──────────┐ │
              │ │ Watcher  │─┼──┘  │ │(无Watcher)│ │
              │ │ (仅A运行) │ │     │ │          │ │
              │ └──────────┘ │     │ └──────────┘ │
              │ ┌──────────┐ │     │ ┌──────────┐ │
              │ │Redis     │◄┼─────┼─┤Redis     │ │
              │ │Relay订阅 │ │     │ │Relay订阅 │ │
              │ └────┬─────┘ │     │ └────┬─────┘ │
              │      │转发    │     │      │转发    │
              │ ┌────▼─────┐ │     │ ┌────▼─────┐ │
              │ │Simple    │ │     │ │Simple    │ │
              │ │Broker    │ │     │ │Broker    │ │
              │ └────┬─────┘ │     │ └────┬─────┘ │
              └──────┼───────┘     └──────┼───────┘
                     │WebSocket            │WebSocket
                ┌────▼───┐            ┌────▼───┐
                │客户端C1 │            │客户端C2 │
                └────────┘            └────────┘
```

### 2.2 核心原理

| 阶段 | 说明 |
|------|------|
| **发布** | Watcher 所在实例调用 `LiveWsRedisRelay.publish()`，将消息发送到 Redis Pub/Sub 频道 |
| **广播** | Redis 将消息推送给所有订阅了该频道的实例（包括发布者自身） |
| **订阅** | 每个实例的 `RedisMessageListenerContainer` 监听匹配的频道模式 |
| **转发** | 收到消息后反序列化，通过 `SimpMessagingTemplate` 转发给本地 STOMP 客户端 |

## 3. 模式一键切换

通过 `application.yml` 中的 `live.websocket.mode` 参数，无需修改代码即可切换部署模式：

```yaml
# application.yml
live:
  websocket:
    # single : 单实例模式，直接通过 SimpleBroker 推送（零延迟，无需额外 Redis 开销）
    # cluster: 集群模式，通过 Redis Pub/Sub 中继到所有实例（支持多实例部署）
    mode: single
```

### 3.1 single 模式（默认）

```
Watcher回调 → LiveWebSocketPublisher.send()
                  │ mode == single
                  ▼
           messagingTemplate.convertAndSend(destination, msg)
                  │
                  ▼
           SimpleBroker → 本地 STOMP 客户端
```

- `LiveWsRedisConfig` 被 `@ConditionalOnProperty` 禁用，不创建 Redis 监听容器
- 零额外延迟，零额外 Redis 连接
- 仅适用于**单实例部署**

### 3.2 cluster 模式

```
Watcher回调 → LiveWebSocketPublisher.send()
                  │ mode == cluster
                  ▼
           redisRelay.publish(destination, msg)
                  │ Redis Pub/Sub
                  ▼
           Redis Server ──广播──► 所有实例 LiveWsRedisRelay.onMessage()
                                        │
                                        ▼
                                 messagingTemplate.convertAndSend(destination, msg)
                                        │
                                        ▼
                                 SimpleBroker → 各实例本地 STOMP 客户端
```

- `LiveWsRedisConfig` 创建 `RedisMessageListenerContainer` 订阅 `x-springboot:live:ws:relay:*`
- 同机房额外延迟约 1-3ms
- 适用于**多实例分布式部署**

### 3.3 切换方式

1. 修改 `application.yml`：`live.websocket.mode: single` 或 `cluster`
2. 重启应用
3. 无需修改任何 Java 代码，无需调整依赖，无需变更部署架构

## 4. 详细消息流程

### 4.1 消息发布流程

```
Watcher回调触发
    │
    ▼
LiveTaskApplicationService.setupCallbacks()
    │ 构建实体 → 写入Buffer → 更新Redis计数器
    │
    ▼
LiveWebSocketPublisher.pushStatistics() / pushNewData() / pushLiveData()
    │ 组装 LiveWsMessage<T>
    │
    ▼
LiveWsRedisRelay.publish(destination, wsMessage)
    │ redisTemplate.convertAndSend(channel, wsMessage)
    │ 序列化方式：RedisTemplate 配置的 Java 原生序列化
    │
    ▼
Redis Server
    │ 匹配频道订阅 → 推送给所有订阅者
    │
    ▼
┌──────────────────────────────────────┐
│  各实例 RedisMessageListenerContainer │
│  收到 Message 回调                     │
│      │                                │
│      ▼                                │
│  LiveWsRedisRelay.onMessage()         │
│      │ 反序列化 body → LiveWsMessage   │
│      │ 从 channel 提取 STOMP目的地     │
│      │                                │
│      ▼                                │
│  SimpMessagingTemplate                │
│      .convertAndSend(destination, msg)│
│      │                                │
│      ▼                                │
│  本地 SimpleBroker → STOMP客户端       │
└──────────────────────────────────────┘
```

### 4.2 客户端订阅流程

```
浏览器客户端
    │
    ├─ SockJS → STOMP CONNECT → WebSocketAuthInterceptor
    │   验证 Redis token → 建立连接
    │
    ├─ SUBSCRIBE /topic/live/{taskId}/statistics
    ├─ SUBSCRIBE /topic/live/{taskId}/chat
    ├─ SUBSCRIBE /topic/live/{taskId}/gift
    ├─ SUBSCRIBE /topic/live/{taskId}/like
    ├─ SUBSCRIBE /topic/live/{taskId}/follow
    ├─ SUBSCRIBE /topic/live/{taskId}/userEnter
    └─ SUBSCRIBE /topic/live/{taskId}/liveData
         │
         ▼
    SimpleBroker（本地内存） 注册订阅关系
         │
         ▼
    当 LiveWsRedisRelay.onMessage() 转发消息时
    → SimpleBroker 匹配订阅 → 投递给对应客户端
```

## 5. REST API 获取实时数据流程（无 WebSocket 依赖）

并非所有场景都需要 WebSocket，REST API 可直接从 Redis 读取：

```
客户端
    │
    │ GET /live/task/statistics/{taskId}
    │ GET /live/task/liveData/{taskId}
    │
    ▼
LiveTaskController
    │
    ▼
LiveTaskApplicationService
    │
    ├─ getStatistics(taskId)
    │      │
    │      ├─ liveRedisCacheService.hasCounters(taskId)
    │      │      │ true → getStatisticsFromHash(taskId) → 返回
    │      │      │ false → DB COUNT查询 → initCountersFromHash(putIfAbsent) → 返回
    │      │
    │      └─ 返回 LiveTaskStatisticsVO
    │
    └─ getRoomLiveData(taskId)
           │
           └─ liveRedisCacheService.getRoomLiveData(taskId)
                  │ Redis Hash 读取 → 返回 LiveRoomLiveDataVO
```

## 6. 组件设计

### 6.1 组件清单

| 组件 | 类型 | 职责 |
|------|------|------|
| `LiveWsProperties` | ConfigurationProperties | 配置 `live.websocket.mode` 控制 single/cluster 切换 |
| `LiveWebSocketPublisher` | Component | 业务层推送入口，根据 mode 选择直连 STOMP 或 Redis 中继 |
| `LiveWsRedisRelay` | Component + MessageListener | 发布消息到 Redis + 接收 Redis 消息并转发 STOMP |
| `LiveWsRedisConfig` | Configuration | `@ConditionalOnProperty(mode=cluster)` 条件装配 Redis 监听容器 |
| `LiveWsMessage<T>` | POJO | 通用消息容器（type + taskId + data），必须实现 Serializable |
| `WebSocketConfig` | Configuration | STOMP 端点 + SimpleBroker 配置（保持不变） |

### 6.2 LiveWsProperties

```java
@Data
@ConfigurationProperties(prefix = "live.websocket")
public class LiveWsProperties {
    private Mode mode = Mode.single;  // 默认单实例模式

    public enum Mode { single, cluster }
}
```

### 6.3 LiveWebSocketPublisher（模式感知）

```java
@Component
public class LiveWebSocketPublisher {
    private final LiveWsProperties wsProperties;
    private final LiveWsRedisRelay redisRelay;
    private final SimpMessagingTemplate messagingTemplate;

    // 3个推送Topic：
    // /topic/live/{taskId}/statistics    — 统计计数器实时更新
    // /topic/live/{taskId}/{type}        — 新数据事件（chat/gift/like/follow/userEnter）
    // /topic/live/{taskId}/liveData      — 在线人数 + 排行榜

    private void send(String destination, LiveWsMessage<?> msg) {
        if (wsProperties.getMode() == LiveWsProperties.Mode.cluster) {
            redisRelay.publish(destination, msg);       // Redis Pub/Sub 中继
        } else {
            messagingTemplate.convertAndSend(destination, msg);  // 直连 SimpleBroker
        }
    }
}
```

### 6.4 LiveWsRedisRelay

```java
@Component
public class LiveWsRedisRelay implements MessageListener {
    // Redis Channel: x-springboot:live:ws:relay:/topic/live/{taskId}/{subTopic}
    //                             └─ 前缀 ─┘└──────── STOMP destination ────────┘

    // 发布侧
    public void publish(String destination, LiveWsMessage<?> wsMessage) {
        redisTemplate.convertAndSend(REDIS_CHANNEL_PREFIX + destination, wsMessage);
    }

    // 订阅侧（MessageListener 回调）
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 1. 反序列化 body → LiveWsMessage
        // 2. 从 channel 提取 STOMP destination（去掉前缀）
        // 3. messagingTemplate.convertAndSend(destination, wsMessage)
    }
}
```

### 6.5 LiveWsRedisConfig（条件装配）

```java
@Configuration
@ConditionalOnProperty(name = "live.websocket.mode", havingValue = "cluster")
public class LiveWsRedisConfig {
    // 仅在 cluster 模式下创建 Redis 监听容器
    // single 模式下此配置被跳过，不占用 Redis 连接
    @Bean
    public RedisMessageListenerContainer liveWsRedisMessageListenerContainer(...) {
        container.addMessageListener(relayListener,
            new PatternTopic(LiveWsRedisRelay.REDIS_CHANNEL_PREFIX + "*"));
    }
}
```

## 7. Redis 频道设计

### 7.1 频道命名规范

| 模式 | 示例 | 说明 |
|------|------|------|
| `{prefix}/topic/live/{taskId}/statistics` | `x-springboot:live:ws:relay:/topic/live/abc123/statistics` | 统计更新 |
| `{prefix}/topic/live/{taskId}/chat` | `x-springboot:live:ws:relay:/topic/live/abc123/chat` | 聊天事件 |
| `{prefix}/topic/live/{taskId}/gift` | `x-springboot:live:ws:relay:/topic/live/abc123/gift` | 礼物事件 |
| `{prefix}/topic/live/{taskId}/like` | `x-springboot:live:ws:relay:/topic/live/abc123/like` | 点赞事件 |
| `{prefix}/topic/live/{taskId}/follow` | `x-springboot:live:ws:relay:/topic/live/abc123/follow` | 关注事件 |
| `{prefix}/topic/live/{taskId}/userEnter` | `x-springboot:live:ws:relay:/topic/live/abc123/userEnter` | 用户进入 |
| `{prefix}/topic/live/{taskId}/liveData` | `x-springboot:live:ws:relay:/topic/live/abc123/liveData` | 在线+排行 |

其中 `prefix = Constant.SYSTEM_NAME + "live:ws:relay:"` = `x-springboot:live:ws:relay:`

### 7.2 设计要点

- **频道与 STOMP destination 一一对应**：频道名 = `前缀 + STOMP destination`，转发时直接去掉前缀即可得到 destination
- **统一前缀**：便于订阅模式匹配（`prefix + "*"` 可匹配所有 live 模块消息）
- **区分任务**：不同 taskId 使用不同频道，避免无关消息投递
- **区分类型**：不同事件类型使用不同频道（statistics / chat / gift / ...）

## 8. 消息格式

### 8.1 LiveWsMessage 结构

```java
public class LiveWsMessage<T> implements Serializable {
    private String type;     // "statistics" | "chat" | "gift" | "like" | "follow" | "userEnter" | "liveData"
    private String taskId;   // 直播任务ID
    private T data;          // 业务数据体：LiveTaskStatisticsVO / LiveChatVO / LiveGiftVO / ...
}
```

### 8.2 序列化

- 使用 **Java 原生序列化**（`RedisTemplate<String, Object>` 配置的 `JdkSerializationRedisSerializer`）
- 与项目中其他 Redis 缓存操作（LiveRedisCacheService）保持一致
- `LiveWsMessage` 及其内部 `T data` 的 VO 类均需实现 `Serializable`（当前已满足）

## 9. 与原有架构的对比

| 维度 | 原方案（SimpleBroker only） | 新方案（Redis Pub/Sub） |
|------|--------------------------|----------------------|
| 单实例 | 正常 | 正常（消息经过 Redis 回到自己，略有延迟） |
| 多实例 | **消息无法跨节点** | 所有实例都能收到并转发 |
| 实例宕机 | WebSocket 断开，消息丢失 | 其他实例继续接收，仅该实例客户端断开 |
| 新增延迟 | 0 | Redis 网络往返 + 反序列化（通常 < 5ms） |
| Redis 依赖 | 仅 token 验证 | token 验证 + Pub/Sub 中继 |
| 复杂度 | 低 | 中 |
| 序列化 | STOMP JSON | Redis Java 序列化 + STOMP JSON（双序列化） |

### 9.1 延迟评估

```
推送路径延迟 = Redis网络RTT + Java反序列化 + STOMP转发
              ≈ 1-3ms（同机房 Redis）
```

对于直播场景的实时性需求（秒级更新），该延迟完全可接受。

## 10. 配置清单

### 10.1 模式切换

```yaml
# application.yml — 一键切换部署模式
live:
  websocket:
    mode: single   # 单实例：SimpleBroker 直连
    # mode: cluster  # 多实例：Redis Pub/Sub 中继
```

### 10.2 已有配置（无需修改）

```yaml
# application.yml
spring:
  data:
    redis:
      host: localhost  # 生产环境指向 Redis 集群
      port: 6379
```

```java
// WebSocketConfig.java（保持不变）
registry.enableSimpleBroker("/topic");          // 保留本地 SimpleBroker
registry.setApplicationDestinationPrefixes("/app");
```

### 10.3 新增配置

```java
// LiveWsRedisConfig.java — 仅 cluster 模式生效（@ConditionalOnProperty）
// single 模式下不创建 RedisMessageListenerContainer，零额外开销
// LiveWsProperties.java — 绑定 live.websocket.* 配置项
```

### 10.4 部署要求

- 所有实例连接**同一个 Redis**（或 Redis 集群），确保 Pub/Sub 广播可达
- Redis 版本无特殊要求，Pub/Sub 为 Redis 基础功能
- 无需额外组件（不需要 RabbitMQ / Kafka / ActiveMQ）

## 11. 注意事项与限制

### 11.1 Redis Pub/Sub 特性

| 特性 | 影响 |
|------|------|
| **无持久化** | Redis Pub/Sub 是即发即忘的，消息不会落盘。客户端离线期间的消息会丢失 |
| **无 ACK** | 发布者不知道哪些订阅者收到了消息，无法重试 |
| **广播模式** | 每个订阅者都会收到全部消息，无法按实例负载定向投递 |
| **连接中断** | 如果某个实例的 Redis 连接中断，该实例上的客户端将收不到消息 |

### 11.2 安全考虑

- Redis 频道中传输的数据可能包含用户昵称、头像 URL、聊天内容等
- 如果 Redis 网络链路不够安全，建议启用 Redis TLS 或使用私有网络

### 11.3 运维监控

建议监控以下指标：

```
# Redis 频道订阅数（应等于实例数）
PUBSUB NUMSUB x-springboot:live:ws:relay:*

# 应用日志关键字
Redis 发布消息失败
Redis 转发消息至 STOMP

# 缓冲区状态（确认无事件积压）
GET /live/task/buffer/status
```

### 11.4 方案局限性

- **消息不保证送达**：Redis Pub/Sub 无持久化无 ACK，极端情况下（实例 Redis 连接中断瞬间）可能丢失消息
  - 缓解措施：关键数据（聊天、礼物等）已先写入 EventBuffer → DB 持久化，WebSocket 仅用于前端实时展示
- **不适合超大集群**：如果实例数 > 50，每个频道消息会广播到所有实例，网络开销 O(n)
  - 缓解措施：如未来实例数增长，可考虑引入 Redis Cluster Pub/Sub 或切换至 Kafka/RabbitMQ

## 12. 回退方案

回退到纯单实例模式最简单的方式：修改配置即可，**无需改代码**。

1. 修改 `application.yml`：`live.websocket.mode: single`
2. 重启应用

如需完全移除 Redis 中继相关代码，可另行删除 `LiveWsRedisRelay.java` 和 `LiveWsRedisConfig.java`。

## 13. 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `LiveWsProperties.java` | 新增 | `@ConfigurationProperties` 绑定 `live.websocket.*`，控制 mode 切换 |
| `LiveWsRedisRelay.java` | 新增 | Redis Pub/Sub 中继器（发布 + 订阅） |
| `LiveWsRedisConfig.java` | 新增 | `@ConditionalOnProperty(mode=cluster)` 条件装配 Redis 监听容器 |
| `LiveWebSocketPublisher.java` | 修改 | 新增 mode 判断，single 直连 STOMP / cluster 走 Redis 中继 |
| `application.yml` | 修改 | 新增 `live.websocket.mode` 配置项 |
| `WebSocketConfig.java` | 不变 | 保留 `enableSimpleBroker("/topic")` |
| `LiveWsMessage.java` | 不变 | 已实现 Serializable，符合要求 |
