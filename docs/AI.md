🚀 你需要操作的 4 步启动

1️⃣  启动 Docker Desktop + PGVector

# 启动 Docker Desktop 后执行
docker run -d --name pgvector \
-e POSTGRES_PASSWORD=postgres123 \
-e POSTGRES_DB=x_springboot_ai \
-p 5432:5432 \
pgvector/pgvector:pg16

2️⃣  初始化 MySQL 表

mysql -u root -proot1234 x_springboot < src/main/resources/db/ai_module_init.sql

3️⃣  设置 API Key 环境变量

# Chat (DeepSeek 或其他)
export AI_CHAT_API_KEY=sk-your-deepseek-key
export AI_CHAT_BASE_URL=https://api.deepseek.com
export AI_CHAT_MODEL=deepseek-chat

# Embedding (硅基流动)
export AI_EMBEDDING_API_KEY=sk-your-siliconflow-key
export AI_EMBEDDING_BASE_URL=https://api.siliconflow.cn
export AI_EMBEDDING_MODEL=BAAI/bge-m3

切换 Provider 示例（换成智谱）：
export AI_CHAT_BASE_URL=https://open.bigmodel.cn/api/paas/v4
export AI_CHAT_API_KEY=你的智谱key
export AI_CHAT_MODEL=glm-4-flash

4️⃣  导入知识库

启动应用后调用：
curl -X POST http://localhost:8080/ai/knowledge/import

  ---
后端菜单注册 SQL

-- 在管理后台菜单表中插入 AI 模块菜单（表名可能是 sys_menu）
INSERT INTO sys_menu (menu_name, parent_id, url, perms, type, icon, order_num)
VALUES ('AI助手', 0, '/ai', '', 0, 'el-icon-cpu', 10);
-- 子菜单由前端路由动态加载

前端路由已由 loadView() 自动处理，URL /ai/knowledge, /ai/chat, /ai/interview
会自动解析到对应视图文件。
