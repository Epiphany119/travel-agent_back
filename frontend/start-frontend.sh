#!/bin/bash
# ================================================
# Roamly 前端一键启动脚本
# ================================================

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================"
echo "  Roamly 前端启动器"
echo "========================================"

# 1. 检查 Node
echo -n "[1/2] 检查 Node..."
if command -v node > /dev/null 2>&1; then
    echo " ✓ $(node -v)"
else
    echo " ✗ 未检测到 Node.js"
    exit 1
fi

# 2. 安装依赖 + 启动
echo "[2/2] 启动 Vite 开发服务器..."
echo "----------------------------------------"
cd "$PROJECT_DIR"

if [ ! -d "node_modules" ]; then
    echo "首次运行，正在安装依赖..."
    npm install
fi

npm run dev
