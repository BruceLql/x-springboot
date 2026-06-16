# 直播监控模块使用说明

## 📋 模块概述

直播监控模块基于DDD架构设计，支持对抖音和TikTok直播间进行实时监控，自动采集聊天、礼物、点赞、关注、用户进入等数据。

## 🎯 核心功能

### 1. 任务管理
- ✅ **创建任务**：添加需要监控的直播间
- ✅ **启动任务**：开始实时监控
- ✅ **停止任务**：随时中断监控
- ✅ **删除任务**：移除监控任务
- ✅ **防重复监控**：同一直播间不能重复创建

### 2. 自动超时控制
- ⏰ **默认时长**：4小时（240分钟）
- ⏰ **最大时长**：8小时（480分钟）
- ⏰ **自动停止**：到达设定时间后自动停止监控
- ⏰ **定时检查**：每分钟检查一次过期任务

### 3. 数据采集配置
创建任务时可按需配置采集数据类型：
- 💬 **聊天数据**（默认开启）
- 🎁 **礼物数据**（默认开启）
- 👍 **点赞数据**（默认关闭）
- ❤️ **关注数据**（默认关闭）
- 👤 **用户进入数据**（默认关闭）

### 4. 数据查询
- 📊 **任务列表**：分页查询所有监控任务
- 📊 **任务详情**：查看任务详细信息
- 📊 **统计数据**：查询任务的数据统计
- 📊 **明细数据**：查询聊天、礼物、点赞、关注、用户进入等明细数据

## 🗂️ 模块结构

```
live/
├── domain/                      # 领域层
│   ├── entity/                  # 领域实体
│   │   ├── LiveTask.java        # 直播任务
│   │   ├── LiveChat.java        # 聊天数据
│   │   ├── LiveGift.java        # 礼物数据
│   │   ├── LiveLike.java        # 点赞数据
│   │   ├── LiveFollow.java      # 关注数据
│   │   └── LiveUserEnter.java   # 用户进入数据
│   ├── enums/                   # 枚举类
│   │   ├── PlatformEnum.java    # 平台枚举
│   │   └── TaskStatusEnum.java  # 任务状态枚举
│   └── command/                 # 命令对象
│       ├── CreateLiveTaskCommand.java
│       ├── StartLiveTaskCommand.java
│       └── StopLiveTaskCommand.java
├── infrastructure/              # 基础设施层
│   ├── repository/              # 数据访问
│   │   ├── LiveTaskMapper.java
│   │   ├── LiveChatMapper.java
│   │   ├── LiveGiftMapper.java
│   │   ├── LiveLikeMapper.java
│   │   ├── LiveFollowMapper.java
│   │   └── LiveUserEnterMapper.java
│   └── convert/                 # 对象转换
│       └── LiveConvert.java
├── application/                 # 应用层
│   └── service/
│       ├── LiveTaskApplicationService.java  # 任务应用服务
│       └── LiveTaskScheduler.java           # 定时任务
└── interfaces/                  # 接口层
    ├── controller/
    │   └── LiveTaskController.java
    ├── dto/
    │   └── query/
    │       ├── LiveTaskPageQuery.java
    │       └── LiveDataPageQuery.java
    └── vo/
        ├── LiveTaskVO.java
        ├── LiveTaskStatisticsVO.java
        ├── LiveChatVO.java
        ├── LiveGiftVO.java
        ├── LiveLikeVO.java
        ├── LiveFollowVO.java
        └── LiveUserEnterVO.java
```

## 📦 数据库初始化

执行以下SQL脚本创建数据表：

```bash
mysql -u root -p your_database < doc/live_module.sql
```

或手动执行 `doc/live_module.sql` 中的SQL语句。

## 🔌 API接口文档

### 1. 任务管理接口

#### 1.1 创建任务
```http
POST /live/task/create
Content-Type: application/json

{
  "platform": "DOUYIN",           // DOUYIN-抖音, TIKTOK-TikTok
  "roomUrl": "https://live.douyin.com/123456",
  "collectChat": true,            // 是否采集聊天数据
  "collectGift": true,            // 是否采集礼物数据
  "collectLike": false,           // 是否采集点赞数据
  "collectFollow": false,         // 是否采集关注数据
  "collectUser": false,           // 是否采集用户进入数据
  "maxDuration": 240              // 最大监控时长(分钟)
}
```

#### 1.2 启动任务
```http
POST /live/task/start/{taskId}
```

#### 1.3 停止任务
```http
POST /live/task/stop/{taskId}
```

#### 1.4 删除任务
```http
DELETE /live/task/delete/{taskId}
```

#### 1.5 查询任务列表
```http
GET /live/task/list?pageNo=1&limit=10&platform=DOUYIN&taskStatus=MONITORING
```

#### 1.6 查询任务详情
```http
GET /live/task/info/{taskId}
```

#### 1.7 查询任务统计
```http
GET /live/task/statistics/{taskId}
```

### 2. 数据查询接口

#### 2.1 查询聊天数据
```http
GET /live/data/chat?pageNo=1&limit=20&taskId=xxx&userId=xxx&userNickname=xxx
```

#### 2.2 查询礼物数据
```http
GET /live/data/gift?pageNo=1&limit=20&taskId=xxx
```

#### 2.3 查询点赞数据
```http
GET /live/data/like?pageNo=1&limit=20&taskId=xxx
```

#### 2.4 查询关注数据
```http
GET /live/data/follow?pageNo=1&limit=20&taskId=xxx
```

#### 2.5 查询用户进入数据
```http
GET /live/data/userEnter?pageNo=1&limit=20&taskId=xxx
```

## 💡 使用示例

### 方式一：通过API接口

1. **创建监控任务**
```bash
curl -X POST http://localhost:8080/live/task/create \
  -H "Content-Type: application/json" \
  -d '{
    "platform": "DOUYIN",
    "roomUrl": "https://live.douyin.com/123456789",
    "collectChat": true,
    "collectGift": true,
    "maxDuration": 240
  }'
```

2. **启动监控**
```bash
curl -X POST http://localhost:8080/live/task/start/{taskId}
```

3. **查看任务列表**
```bash
curl http://localhost:8080/live/task/list?pageNo=1&limit=10
```

4. **查看统计数据**
```bash
curl http://localhost:8080/live/task/statistics/{taskId}
```

5. **查询聊天数据**
```bash
curl http://localhost:8080/live/data/chat?pageNo=1&limit=20&taskId={taskId}
```

6. **停止监控**
```bash
curl -X POST http://localhost:8080/live/task/stop/{taskId}
```

### 方式二：通过代码调用

```java
@Autowired
private LiveTaskApplicationService liveTaskApplicationService;

// 1. 创建任务
CreateLiveTaskCommand command = new CreateLiveTaskCommand();
command.setPlatform(PlatformEnum.DOUYIN);
command.setRoomUrl("https://live.douyin.com/123456789");
command.setCollectChat(true);
command.setCollectGift(true);
command.setCollectLike(false);
command.setCollectFollow(false);
command.setCollectUser(false);
command.setMaxDuration(240);

LiveTaskVO task = liveTaskApplicationService.create(command);

// 2. 启动任务
liveTaskApplicationService.startTask(task.getTaskId());

// 3. 查询任务列表
LiveTaskPageQuery query = new LiveTaskPageQuery();
query.setPageNo(1);
query.setLimit(10);
IPage<LiveTaskVO> page = liveTaskApplicationService.queryPage(query);

// 4. 查询统计数据
LiveTaskStatisticsVO statistics = liveTaskApplicationService.getStatistics(task.getTaskId());

// 5. 停止任务
liveTaskApplicationService.stopTask(task.getTaskId());
```

## 🔧 技术特性

### 1. DDD分层架构
- **领域层**：实体、枚举、命令对象
- **基础设施层**：Mapper、转换器
- **应用层**：应用服务、定时任务
- **接口层**：Controller、DTO、VO

### 2. 并发控制
- 使用 `ConcurrentHashMap` 存储运行中的监控任务
- 线程池管理监控线程
- 任务状态机控制（PENDING → MONITORING → STOPPED/EXPIRED）

### 3. 数据一致性
- 使用 `@Transactional` 保证事务一致性
- 雪花算法生成唯一ID
- MapStruct进行对象转换

### 4. 定时任务
- Spring `@Scheduled` 实现过期任务自动停止
- Cron表达式：`0 * * * * ?`（每分钟执行）

## 📊 任务状态流转

```
创建任务 → PENDING(待监控)
           ↓
         启动任务
           ↓
      MONITORING(监控中)
           ↓
    ┌──────┴──────┐
    ↓             ↓
  手动停止     自动超时
    ↓             ↓
 STOPPED(已停止) EXPIRED(已过期)
```

## ⚠️ 注意事项

1. **防止重复监控**
   - 系统会自动检查同一URL是否已存在PENDING或MONITORING状态的任务
   - 重复创建会抛出异常

2. **超时控制**
   - 最大监控时长不能超过480分钟（8小时）
   - 到达设定时间后系统会自动停止任务

3. **依赖库**
   - 确保pom.xml中已引入 `live-room-watcher` 依赖
   - 支持抖音和TikTok两个平台

4. **线程管理**
   - 监控任务在独立线程中运行
   - 停止任务时会正确中断线程并释放资源

5. **数据量控制**
   - 实时监控会产生大量数据
   - 建议定期清理历史数据
   - 只开启需要的数据采集类型

## 🚀 扩展建议

1. **数据清理**
   - 添加定时任务清理7天前的历史数据
   - 或采用分表策略按月存储

2. **告警通知**
   - 任务异常时发送钉钉/企业微信通知
   - 监控数据达到阈值时告警

3. **数据导出**
   - 支持导出Excel格式的直播数据报表
   - 支持按时间段导出数据

4. **实时监控大屏**
   - WebSocket推送实时数据
   - 前端展示实时聊天、礼物等信息

5. **权限控制**
   - 添加租户隔离
   - 角色权限控制

## 📝 常见问题

### Q1: 任务启动失败？
**A**: 检查直播间URL是否正确，网络是否通畅，直播间是否存在。

### Q2: 数据没有采集到？
**A**: 检查创建任务时是否开启了对应的数据采集配置。

### Q3: 如何修改超时检查频率？
**A**: 修改 `LiveTaskScheduler.java` 中的 `@Scheduled` cron表达式。

### Q4: 支持哪些平台？
**A**: 目前支持抖音（DOUYIN）和TikTok（TIKTOK）两个平台。

## 📞 技术支持

如有问题，请查看日志文件：
```bash
tail -f logs/application.log | grep "直播"
```

或检查控制台输出的详细错误信息。
