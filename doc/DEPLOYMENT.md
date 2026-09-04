# 部署文档

## 目录

1. [环境准备](#1-环境准备)
2. [PR#1: 单体部署](#2-pr1-单体部署)
3. [PR#3: MCP Server 集群部署](#3-pr3-mcp-server-集群部署)
4. [Docker Compose 部署](#4-docker-compose-部署)
5. [Kubernetes 部署](#5-kubernetes-部署)
6. [运维指南](#6-运维指南)
7. [常见问题](#7-常见问题)

---

## 1. 环境准备

### 1.1 系统要求

| 组件 | 最低版本 | 推荐版本 |
|------|----------|----------|
| JDK | 17 | 17 LTS |
| Maven | 3.8 | 3.9+ |
| Node.js | 18 | 20 LTS |
| MySQL | 8.0 | 8.0.33+ |
| Docker | 20.10 | 24.0+ |
| Kubernetes | 1.25 | 1.28+ |

### 1.2 必需的服务

| 服务 | 版本 | 端口 | 说明 |
|------|------|------|------|
| MySQL | 8.0 | 3306 | 数据库 |

### 1.3 第三方 API Key

| 服务 | 申请地址 | 用途 | 配额 |
|------|----------|------|------|
| 智谱 AI | https://open.bigmodel.cn | LLM 调用 | 免费版 200元 |
| 和风天气 | https://www.qweather.com | 天气预报 | 免费版 800次/日 |
| 高德地图 | https://lbs.amap.com | POI/路线 | 免费版 5000次/日 |

---

## 2. PR#1: 单体部署

### 2.1 快速启动

```bash
# 1. 克隆项目
git clone <repository-url>
cd travel-agent

# 2. 创建本地配置
cat > travel-web/src/main/resources/application-local.yaml << EOF
spring:
  ai:
    openai:
      api-key: your-zhipu-api-key
  datasource:
    username: root
    password: your-mysql-password

travel:
  weather:
    api-key: your-weather-api-key
  amap:
    api-key: your-amap-api-key
EOF

# 3. 编译项目
mvn clean package -DskipTests

# 4. 启动数据库 (使用 Docker)
docker run -d \
  --name travel-mysql \
  -e MYSQL_ROOT_PASSWORD=your-mysql-password \
  -e MYSQL_DATABASE=travel_agent \
  -p 3306:3306 \
  mysql:8.0

# 5. 启动主服务
cd travel-web && mvn spring-boot:run
```

### 2.2 验证部署

```bash
# 健康检查
curl http://localhost:8080/api/travel/health

# 测试天气接口
curl "http://localhost:8080/api/travel/weather?city=北京"

# 测试旅行规划
curl -X POST http://localhost:8080/api/travel/plan \
  -H "Content-Type: application/json" \
  -d '{"request": "帮我规划北京3日游"}'
```

---

## 3. PR#3: MCP Server 集群部署

### 3.1 服务概览

PR#3 将 MCP 功能拆分为 4 个独立服务：

| 服务 | 端口 | 工具数 | 外部依赖 |
|------|------|--------|----------|
| travel-mcp-server-weather | 8081 | 1 | 和风天气 API |
| travel-mcp-server-poi | 8082 | 9 | 高德地图 API |
| travel-mcp-server-meal | 8083 | 1 | 委托 POI Server |
| travel-mcp-server-budget | 8084 | 1 | 无 |

### 3.2 启动 MCP Server 集群

```bash
# 1. 编译项目
mvn clean package -DskipTests

# 2. 启动 MCP Server 集群 (4个服务并行)
cd travel-mcp-server-weather && mvn spring-boot:run &
cd travel-mcp-server-poi && mvn spring-boot:run &
cd travel-mcp-server-meal && mvn spring-boot:run &
cd travel-mcp-server-budget && mvn spring-boot:run &

# 3. 启动主服务
cd travel-web && mvn spring-boot:run
```

### 3.3 快速启动脚本

创建 `start-mcp-cluster.sh`:

```bash
#!/bin/bash

echo "Starting Travel Agent MCP Server Cluster..."

# 1. 启动 MCP Server 集群
for server in weather poi meal budget; do
  echo "Starting $server server..."
  (cd travel-mcp-server-$server && mvn spring-boot:run &)
done

# 等待 MCP Server 启动
echo "Waiting for MCP Servers to start..."
sleep 15

# 2. 验证 MCP Server
echo "Verifying MCP Servers..."
for port in 8081 8082 8083 8084; do
  echo -n "Checking localhost:$port ... "
  if curl -s -f "http://localhost:$port/health" > /dev/null 2>&1; then
    echo "OK"
  else
    echo "FAILED"
  fi
done

# 3. 启动主服务
echo "Starting travel-web..."
cd travel-web && mvn spring-boot:run
```

### 3.4 MCP Server 配置

#### Weather Server (8081)

```yaml
# travel-mcp-server-weather/src/main/resources/application.yml
server:
  port: 8081

spring:
  application:
    name: travel-mcp-server-weather

travel:
  weather:
    api-key: ${WEATHER_API_KEY:}
```

#### POI Server (8082)

```yaml
# travel-mcp-server-poi/src/main/resources/application.yml
server:
  port: 8082

spring:
  application:
    name: travel-mcp-server-poi

travel:
  amap:
    api-key: ${GAODE_API_KEY:}
```

#### Meal Server (8083)

```yaml
# travel-mcp-server-meal/src/main/resources/application.yml
server:
  port: 8083

spring:
  application:
    name: travel-mcp-server-meal

travel:
  meal:
    poi-server-url: ${POI_MCP_URL:http://localhost:8082}
```

#### Budget Server (8084)

```yaml
# travel-mcp-server-budget/src/main/resources/application.yml
server:
  port: 8084

spring:
  application:
    name: travel-mcp-server-budget
```

### 3.5 MCP Server 健康检查

```bash
# 检查所有 MCP Server 健康状态
for port in 8081 8082 8083 8084; do
  echo -n "MCP Server :$port ... "
  curl -s "http://localhost:$port/health"
  echo ""
done
```

---

## 4. Docker Compose 部署

### 4.1 docker-compose.yml

```yaml
version: '3.8'

services:
  # MySQL 数据库
  mysql:
    image: mysql:8.0
    container_name: travel-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_DATABASE: travel_agent
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Weather MCP Server (8081)
  weather-server:
    build:
      context: ./travel-mcp-server-weather
    container_name: travel-weather
    ports:
      - "8081:8081"
    environment:
      SERVER_PORT: 8081
      WEATHER_API_KEY: ${WEATHER_API_KEY}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/health"]
      interval: 10s
      timeout: 5s
      retries: 3

  # POI MCP Server (8082)
  poi-server:
    build:
      context: ./travel-mcp-server-poi
    container_name: travel-poi
    ports:
      - "8082:8082"
    environment:
      SERVER_PORT: 8082
      GAODE_API_KEY: ${GAODE_API_KEY}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/health"]
      interval: 10s
      timeout: 5s
      retries: 3

  # Meal MCP Server (8083)
  meal-server:
    build:
      context: ./travel-mcp-server-meal
    container_name: travel-meal
    ports:
      - "8083:8083"
    environment:
      SERVER_PORT: 8083
      POI_MCP_URL: http://poi-server:8082
    depends_on:
      poi-server:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8083/health"]
      interval: 10s
      timeout: 5s
      retries: 3

  # Budget MCP Server (8084)
  budget-server:
    build:
      context: ./travel-mcp-server-budget
    container_name: travel-budget
    ports:
      - "8084:8084"
    environment:
      SERVER_PORT: 8084
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8084/health"]
      interval: 10s
      timeout: 5s
      retries: 3

  # 主 Web 服务 (8080)
  travel-web:
    build:
      context: .
      dockerfile: Dockerfile.web
    container_name: travel-web
    ports:
      - "8080:8080"
    environment:
      SERVER_PORT: 8080
      ZHIPU_API_KEY: ${ZHIPU_API_KEY}
      WEATHER_API_KEY: ${WEATHER_API_KEY}
      GAODE_API_KEY: ${GAODE_API_KEY}
      TRAVEL_DB_URL: jdbc:mysql://mysql:3306/travel_agent
      TRAVEL_DB_USERNAME: root
      TRAVEL_DB_PASSWORD: ${MYSQL_PASSWORD}
      WEATHER_MCP_URL: http://weather-server:8081
      POI_MCP_URL: http://poi-server:8082
      MEAL_MCP_URL: http://meal-server:8083
      BUDGET_MCP_URL: http://budget-server:8084
    depends_on:
      mysql:
        condition: service_healthy
      weather-server:
        condition: service_healthy
      poi-server:
        condition: service_healthy
      meal-server:
        condition: service_healthy
      budget-server:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/travel/health"]
      interval: 10s
      timeout: 5s
      retries: 3

volumes:
  mysql_data:
```

### 4.2 .env 文件

```bash
# 数据库
MYSQL_PASSWORD=your-secure-password

# API Keys
ZHIPU_API_KEY=your-zhipu-key
WEATHER_API_KEY=your-weather-key
GAODE_API_KEY=your-gaode-key
```

### 4.3 启动命令

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f travel-web

# 停止服务
docker-compose down
```

---

## 5. Kubernetes 部署

### 5.1 ConfigMap

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: travel-agent-config
data:
  WEATHER_MCP_URL: "http://weather-server:8081"
  POI_MCP_URL: "http://poi-server:8082"
  MEAL_MCP_URL: "http://meal-server:8083"
  BUDGET_MCP_URL: "http://budget-server:8084"
  TRAVEL_DB_URL: "jdbc:mysql://mysql:3306/travel_agent"
```

### 5.2 Secret

```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: travel-agent-secrets
type: Opaque
stringData:
  ZHIPU_API_KEY: "your-zhipu-key"
  WEATHER_API_KEY: "your-weather-key"
  GAODE_API_KEY: "your-gaode-key"
  MYSQL_PASSWORD: "your-secure-password"
```

### 5.3 Deployment 清单

#### Weather Server

```yaml
# deployment-weather.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: weather-server
  labels:
    app: travel-agent
    component: weather-server
spec:
  replicas: 2
  selector:
    matchLabels:
      app: travel-agent
      component: weather-server
  template:
    metadata:
      labels:
        app: travel-agent
        component: weather-server
    spec:
      containers:
        - name: weather-server
          image: travel-mcp-server-weather:latest
          ports:
            - containerPort: 8081
          envFrom:
            - secretRef:
                name: travel-agent-secrets
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /health
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /health
              port: 8081
---
apiVersion: v1
kind: Service
metadata:
  name: weather-server
spec:
  selector:
    app: travel-agent
    component: weather-server
  ports:
    - port: 8081
      targetPort: 8081
```

#### POI Server

```yaml
# deployment-poi.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: poi-server
spec:
  replicas: 3  # POI 服务可能访问量更大
  selector:
    matchLabels:
      app: travel-agent
      component: poi-server
  template:
    spec:
      containers:
        - name: poi-server
          image: travel-mcp-server-poi:latest
          ports:
            - containerPort: 8082
          envFrom:
            - secretRef:
                name: travel-agent-secrets
          resources:
            requests:
              memory: "512Mi"
              cpu: "200m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: poi-server
spec:
  selector:
    app: travel-agent
    component: poi-server
  ports:
    - port: 8082
      targetPort: 8082
```

#### Meal Server

```yaml
# deployment-meal.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: meal-server
spec:
  replicas: 2
  selector:
    matchLabels:
      app: travel-agent
      component: meal-server
  template:
    spec:
      containers:
        - name: meal-server
          image: travel-mcp-server-meal:latest
          ports:
            - containerPort: 8083
          env:
            - name: POI_MCP_URL
              value: "http://poi-server:8082"
---
apiVersion: v1
kind: Service
metadata:
  name: meal-server
spec:
  selector:
    app: travel-agent
    component: meal-server
  ports:
    - port: 8083
      targetPort: 8083
```

#### Budget Server

```yaml
# deployment-budget.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: budget-server
spec:
  replicas: 1  # 纯内存计算，无需高可用
  selector:
    matchLabels:
      app: travel-agent
      component: budget-server
  template:
    spec:
      containers:
        - name: budget-server
          image: travel-mcp-server-budget:latest
          ports:
            - containerPort: 8084
          resources:
            requests:
              memory: "128Mi"
              cpu: "50m"
---
apiVersion: v1
kind: Service
metadata:
  name: budget-server
spec:
  selector:
    app: travel-agent
    component: budget-server
  ports:
    - port: 8084
      targetPort: 8084
```

#### 主 Web 服务

```yaml
# deployment-web.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: travel-web
spec:
  replicas: 3
  selector:
    matchLabels:
      app: travel-agent
      component: web
  template:
    spec:
      containers:
        - name: travel-web
          image: travel-agent:latest
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: travel-agent-config
            - secretRef:
                name: travel-agent-secrets
---
apiVersion: v1
kind: Service
metadata:
  name: travel-web
spec:
  type: LoadBalancer
  selector:
    app: travel-agent
    component: web
  ports:
    - port: 80
      targetPort: 8080
```

---

## 6. 运维指南

### 6.1 日志查看

```bash
# Docker 日志
docker-compose logs -f travel-web
docker-compose logs -f weather-server
docker-compose logs -f poi-server

# Kubernetes 日志
kubectl logs -f deployment/weather-server
kubectl logs -f deployment/poi-server
kubectl logs -f deployment/travel-web
```

### 6.2 监控检查

```bash
# 检查所有服务健康状态
echo "Checking MCP Server cluster..."
for port in 8081 8082 8083 8084; do
  echo -n "  :$port ... "
  curl -s -f "http://localhost:$port/health" > /dev/null && echo "OK" || echo "FAILED"
done

echo "Checking Web service..."
curl -s -f "http://localhost:8080/api/travel/health" > /dev/null && echo "OK" || echo "FAILED"
```

### 6.3 扩缩容

```bash
# Kubernetes 扩缩容
kubectl scale deployment weather-server --replicas=4
kubectl scale deployment poi-server --replicas=6
kubectl scale deployment travel-web --replicas=5

# Docker Compose 扩缩容
docker-compose up -d --scale weather-server=4
```

### 6.4 滚动更新

```bash
# 构建新版本镜像
docker build -t travel-agent:v2.0 .

# Kubernetes 滚动更新
kubectl set image deployment/weather-server weather-server=travel-weather:v2.0
kubectl set image deployment/poi-server poi-server=travel-poi:v2.0
kubectl set image deployment/travel-web web=travel-agent:v2.0

# 查看更新进度
kubectl rollout status deployment/travel-web

# 回滚
kubectl rollout undo deployment/travel-web
```

### 6.5 备份恢复

```bash
# MySQL 备份
mysqldump -u root -p travel_agent > backup_$(date +%Y%m%d).sql

# MySQL 恢复
mysql -u root -p travel_agent < backup_20260812.sql
```

---

## 7. 常见问题

### 7.1 MCP Server 连接失败

**问题**: 主服务无法连接 MCP Server

**解决**:
1. 检查 MCP Server 是否正常启动: `docker-compose ps`
2. 检查端口是否被占用: `lsof -i :8081`
3. 查看日志: `docker-compose logs weather-server`
4. 验证网络连通性: `docker-compose exec travel-web ping weather-server`

### 7.2 API Key 问题

**问题**: 返回 "API Key 无效"

**解决**:
1. 确认 API Key 正确配置在 `.env` 文件
2. 检查环境变量是否正确注入
3. 验证 API Key 是否过期或额度用完

### 7.3 数据库连接

**问题**: 连接 MySQL 超时

**解决**:
1. 确认 MySQL 已启动: `docker-compose ps mysql`
2. 检查网络连通性: `docker-compose exec travel-web ping mysql`
3. 验证数据库凭证

### 7.4 限流触发

**问题**: 返回限流错误

**解决**:
1. 查看限流状态: `curl http://localhost:8080/api/ratelimit/status`
2. 等待次日配额重置
3. 升级 API 服务套餐

---

## 附录

### A. 完整启动脚本

创建 `scripts/start-all.sh`:

```bash
#!/bin/bash
set -e

echo "=========================================="
echo "Travel Agent - Starting All Services"
echo "=========================================="

# 1. 启动数据库
echo "[1/4] Starting MySQL..."
docker run -d \
  --name travel-mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=travel_agent \
  -p 3306:3306 \
  mysql:8.0

sleep 10

# 2. 启动 MCP Server 集群
echo "[2/4] Starting MCP Server Cluster..."
for server in weather poi meal budget; do
  echo "  - Starting $server server..."
  (cd travel-mcp-server-$server && mvn spring-boot:run &)
done

echo "  Waiting for MCP Servers..."
sleep 15

# 3. 启动主服务
echo "[3/4] Starting travel-web..."
cd travel-web && mvn spring-boot:run

echo "[4/4] All services started!"
echo "=========================================="
```

### B. 健康检查脚本

创建 `scripts/health-check.sh`:

```bash
#!/bin/bash

services=(
  "http://localhost:8080/api/travel/health"
  "http://localhost:8081/health"
  "http://localhost:8082/health"
  "http://localhost:8083/health"
  "http://localhost:8084/health"
)

echo "Travel Agent Health Check"
echo "========================="

all_ok=true
for service in "${services[@]}"; do
  echo -n "$(basename $service): "
  if curl -s -f "$service" > /dev/null 2>&1; then
    echo "OK"
  else
    echo "FAILED"
    all_ok=false
  fi
done

echo "========================="
if $all_ok; then
  echo "All services are healthy!"
  exit 0
else
  echo "Some services are down!"
  exit 1
fi
```
