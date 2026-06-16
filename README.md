<h1> X-SpringBoot </h1>

![Image text](https://img.shields.io/badge/x--springboot-v6.0-green.svg)
![Image text](https://img.shields.io/badge/springboot-3.5.11-green.svg)
![Image text](https://img.shields.io/badge/MyBatis%20Plus-3.5.12-green.svg)
![Image text](https://img.shields.io/badge/Java-25-orange.svg)

[更新日志](doc/updateLog.md) | [项目地址](https://github.com/BruceLql/x-springboot) | [前台项目](https://github.com/BruceLql/x-springboot-ui)


## 项目说明

X-SpringBoot 是一个轻量级的 Java 快速开发平台，基于 Spring Boot 3.5 构建，用于快速构建中小型 API、RESTful API 项目。经过多个企业级项目实践验证，稳定、简单、快速，让开发者摆脱重复劳动。

### 核心特性

- **Spring Boot 3.5** — 最新代框架，Java 25 支持
- **MyBatis-Plus 3.5** — 大幅简化 SQL，分页插件、枚举处理器内置
- **Spring Security 6.x** — 自定义 Token 认证 + RBAC 接口级权限控制
- **双数据源架构** — MySQL（业务主库）+ PostgreSQL/PGVector（AI 向量存储）
- **DDD 分层架构** — domain / application / infrastructure / interfaces 四层分离
- **MapStruct** — 编译期对象转换，零反射开销
- **Redisson** — 分布式锁，高并发安全
- **前后端分离** — 前端 Vue 2 独立部署，后端纯 RESTful API
- **SpringDoc OpenAPI** — 接口文档自动生成

### 功能模块

| 模块 | 说明 | 架构 |
|------|------|------|
| **AI 对话 & 知识库** | RAG 检索增强生成、SSE 流式响应、PGVector 向量存储、面试模拟、多 Provider 切换（DeepSeek/ZhipuAI/SiliconFlow） | DDD |
| **直播监控** | 抖音/TikTok 实时数据采集（聊天/礼物/点赞/关注/进入）、WebSocket 实时推送、Redis Pub/Sub 集群中继、电商下单统计 | DDD |
| **货仓管理 (WMS)** | 货物管理、供应商管理、入库/出库、库存三态模型（实际/锁定/占用）、SKU 组合搭配、盘点管理、库存事件流水 | DDD |
| **RBAC 权限** | 用户/角色/菜单管理、接口级权限控制、Token 认证、验证码登录、操作日志/登录日志 | 传统分层 |
| **短链服务** | 短链接生成、分库分表（16 库 16 表）、访问统计（地域/设备/浏览器/网络） | 传统分层 |
| **文件存储 (OSS)** | MinIO / 阿里云 OSS / 七牛云 — 统一抽象层，配置切换 | 传统分层 |
| **开放 API** | 应用管理（appKey/appSecret）、网关路由、IP 限流、流量控制、访问日志 | 传统分层 |
| **短信服务** | 多渠道短信（阿里云）、多模板管理、按月分表 | 传统分层 |
| **多租户** | 租户数据隔离、模块订阅 | 传统分层 |
| **APK 管理** | APK 文件解析、版本管理 | 传统分层 |

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 核心框架 | Spring Boot | 3.5.11 |
| 安全框架 | Spring Security | 6.x |
| ORM | MyBatis-Plus | 3.5.12 |
| 数据库 | MySQL | 9.4.0 驱动 |
| 向量存储 | PostgreSQL + PGVector | pgvector/pgvector:pg16 |
| 缓存 | Redis + Redisson | 3.27.0 |
| AI 框架 | Spring AI | 1.1.5 |
| 对象转换 | MapStruct | 1.5.5 |
| 工具集 | Hutool | 5.8.21 |
| 分布式 ID | Leaf-Snowflake | — |
| 文件存储 | MinIO / 阿里云 OSS / 七牛云 | — |
| 接口文档 | SpringDoc OpenAPI | 2.2.0 |
| 实时通信 | WebSocket (STOMP) | — |
| 前端 | Vue 2.6 + Element UI 2.15 | — |

## 环境要求

- **JDK** 25+
- **MySQL** 8.0+
- **Redis** 6.0+
- **PostgreSQL** 16+（AI 模块需要）
- **Node.js** 12-16（前端开发）

## 项目结构

```
x-springboot
├─doc/                         # 数据库初始化脚本 & 模块文档
│  ├─x_springboot.sql          # 完整数据库 DDL + 种子数据
│  ├─wms.sql                   # 货仓管理表结构
│  ├─wms_schema.sql            # 货仓管理建表（带依赖排序）
│  ├─live_module.sql           # 直播监控表结构
│  ├─ai_module_init.sql        # AI 模块初始化
│  ├─live_module_readme.md     # 直播模块文档
│  └─updateLog.md              # 版本更新日志
│
├─docs/                        # 技术文档
│  ├─AI.md                     # AI 模块快速开始
│  ├─WebSocket-Redis-PubSub-中继方案.md
│  ├─直播数据采集优化方案.md
│  └─前端修改说明.md
│
├─src/main/java/com/suke/czx/
│  ├─authentication/           # Spring Security 认证授权
│  │  ├─detail/                # UserDetailsService 实现
│  │  ├─handler/               # 登录成功/失败/登出处理器
│  │  ├─provider/              # RBAC 授权管理器
│  │  └─utils/                 # Token 工具类
│  │
│  ├─common/                   # 公共模块
│  │  ├─annotation/            # @AuthIgnore、@ResourceAuth
│  │  ├─aspect/                # 系统日志 AOP
│  │  ├─base/                  # Base 基类
│  │  ├─event/                 # 异步事件（登录日志）
│  │  ├─exception/             # 全局异常处理
│  │  ├─lock/                  # 分布式锁（Redisson）
│  │  ├─shardingtable/         # 分表策略
│  │  └─utils/                 # 通用工具类
│  │
│  ├─config/                   # Spring 配置
│  │  └─datasource/            # 双数据源配置（MySQL + PostgreSQL）
│  │
│  ├─interceptor/              # Token 拦截器 / 验证码过滤器 / WebSocket 认证
│  │
│  └─modules/                  # 业务功能模块
│     ├─ai/                    # AI 对话 & 知识库（DDD）
│     ├─apk/                   # APK 管理
│     ├─application/           # 开放 API 应用管理
│     ├─live/                  # 直播监控（DDD）
│     ├─msg/                   # 短信服务
│     ├─oss/                   # 文件存储
│     ├─param/                 # 系统参数
│     ├─shortLink/             # 短链服务（分库分表）
│     ├─sys/                   # RBAC 权限系统
│     ├─tenancy/               # 多租户
│     └─warehouse/             # 货仓管理（DDD）
│
└─src/main/resources/
   └─mapper/                   # MyBatis XML 映射文件
```

## 快速开始

### 1. 环境准备

确保已安装 JDK 25、MySQL 8.0+、Redis、PostgreSQL 16+。

### 2. 初始化数据库

```bash
# 导入基础数据
mysql -u root -p < doc/x_springboot.sql

# 导入直播模块（可选）
mysql -u root -p < doc/live_module.sql

# AI 模块初始化（可选）
mysql -u root -p < doc/ai_module_init.sql
```

### 3. 配置环境变量

```bash
cp .env.properties .env
# 编辑 .env 填入 AI API Key 等配置
```

### 4. 启动后端

```bash
# IDEA 中直接运行 Application.java
# 或命令行
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`

### 5. 启动前端（可选）

```bash
cd ../x-springboot-ui
npm install
npm run dev
```

前端默认运行在 `http://localhost:9999`，代理 API 请求到 `localhost:8080`

### 6. 登录

- 账号：`admin`
- 密码：`admin`

### 7. API 文档

启动后访问 `http://localhost:8080/swagger-ui/index.html`

## 货仓管理系统 (WMS)

### 库存三态模型

```
实际库存 (real_quantity)
   ↓ 组商品创建时锁定
锁定库存 (withhold_quantity) ──→ 取消布款时释放
   ↓ 确定布款时占用
占用库存 (occupy_quantity) ──→ 布款回滚时恢复
   ↓ 出库确认
出库扣减
```

### 技术特点

- **DDD 分层架构**：domain（领域层）/ application（应用层）/ infrastructure（基础设施层）/ interfaces（接口层）
- **MapStruct 对象转换**：统一在 Convert 类中处理 DTO/VO/Entity 转换
- **乐观锁并发控制**：使用 @Version 注解实现库存并发安全
- **库存事件流水**：记录所有库存变更操作，支持审计追踪

## AI 模块

基于 Spring AI + PGVector 构建的 RAG（检索增强生成）系统：

- **知识库管理**：文档上传 → 智能分块（chunk） → 向量嵌入（1024 维） → HNSW 索引
- **AI 对话**：SSE 流式渲染、上下文窗口管理（最近 N 轮历史）、知识库语义检索增强
- **面试模拟**：自动出题、深度追问、智能评分
- **多 Provider 支持**：DeepSeek / ZhipuAI / SiliconFlow，通过环境变量切换

详见 [docs/AI.md](docs/AI.md)

## 直播监控模块

- **多平台采集**：抖音、TikTok 实时数据采集
- **WebSocket 推送**：单实例 SimpleBroker（零延迟） / 集群 Redis Pub/Sub 中继
- **电商数据**：下单计数统计、商品分类
- **任务管理**：监控任务 CRUD、状态追踪（RUNNING / STOPPED / INTERRUPTED）

详见 [doc/live_module_readme.md](doc/live_module_readme.md) 和 [docs/直播数据采集优化方案.md](docs/直播数据采集优化方案.md)

## 贡献

如果您发现了任何问题或有改进建议，请创建 [Issue](https://github.com/BruceLql/x-springboot/issues/new) 或提交 PR。欢迎贡献！

## 致谢

本项目基于 [yzcheng90/X-SpringBoot](https://github.com/yzcheng90/X-SpringBoot) 二次开发，感谢原作者的贡献。
