#!/bin/bash

# 项目根目录（自动检测）
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR"
FRONTEND_DIR="$(dirname "$SCRIPT_DIR")/x-springboot-ui"

echo "🔄 开始重启前后端项目..."

# 1. 关闭所有进程
echo "📌 Step 1: 关闭所有运行中的进程..."
pkill -f "vue-cli-service serve" 2>/dev/null
lsof -ti:8080 | xargs kill -9 2>/dev/null
ps aux | grep "vue-cli-service" | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null
sleep 2

# 2. 编译后端
echo "📌 Step 2: 清理并编译后端..."
cd "$BACKEND_DIR"
if command -v /usr/libexec/java_home &> /dev/null; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 25 2>/dev/null || echo "$JAVA_HOME")
fi
mvn clean compile -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ 后端编译成功"
else
    echo "❌ 后端编译失败,请检查错误信息"
    exit 1
fi

# 3. 启动前端
echo "📌 Step 3: 启动前端..."
if [ -d "$FRONTEND_DIR" ]; then
    cd "$FRONTEND_DIR"
    nohup npm run dev > /tmp/frontend.log 2>&1 &
    echo "✅ 前端已启动: http://localhost:9999"
else
    echo "⚠️ 前端目录不存在: $FRONTEND_DIR，跳过启动"
fi

# 4. 提示启动后端
echo "📌 Step 4: 请手动在 IDEA 中启动后端"
echo "   或运行: cd $BACKEND_DIR && nohup mvn spring-boot:run > /tmp/backend.log 2>&1 &"

echo "🎉 重启完成!"
