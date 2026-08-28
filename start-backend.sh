#!/bin/bash
# ================================================
# Roamly 后端一键启动脚本
# 自动检查并启动 Redis，然后启动 Spring Boot
# ================================================

set -e

REDIS_CLI="/usr/local/bin/redis-cli"
REDIS_SERVER="/usr/local/bin/redis-server"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================"
echo "  Roamly 后端启动器"
echo "========================================"

# 1. 检查 Redis
echo -n "[1/3] 检查 Redis..."
if $REDIS_CLI ping > /dev/null 2>&1; then
    echo " ✓ 已运行"
else
    echo -n " 启动中..."
    if [ -x "$REDIS_SERVER" ]; then
        $REDIS_SERVER --daemonize yes > /dev/null 2>&1
        sleep 1
        if $REDIS_CLI ping > /dev/null 2>&1; then
            echo " ✓ 启动成功"
        else
            echo " ✗ 启动失败"
            exit 1
        fi
    else
        echo " ✗ redis-server 不存在于 $REDIS_SERVER"
        exit 1
    fi
fi

# 2. 检查 MySQL
echo -n "[2/3] 检查 MySQL..."
if mysql -u root -e "SELECT 1" > /dev/null 2>&1 || mysql -u epiphany -p123456 -e "SELECT 1" > /dev/null 2>&1; then
    echo " ✓ 已运行"
else
    echo " ⚠ 未检测到 MySQL（部分功能可能不可用）"
fi

# 3. 启动 Spring Boot
echo "[3/3] 启动 Spring Boot..."
echo "----------------------------------------"
cd "$PROJECT_DIR"
# 同时构建并带入所有依赖模块，避免 travel-web 使用旧的业务模块 class/jar。
mvn spring-boot:run -pl travel-web -am
