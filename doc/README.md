# Travel Agent - 智能旅行规划系统

## 项目概述

Travel Agent 是一个基于 MCP (Model Context Protocol) 协议构建的智能旅行规划 AI Agent 系统。系统通过模块化架构提供天气查询、POI 搜索、餐饮推荐、预算估算、行程规划等功能。

## 技术栈

| 分类 | 技术 |
|------|------|
| **核心框架** | Spring Boot 3.2.4, Spring WebFlux |
| **AI 能力** | Spring AI 1.0.0-M4, 智谱 GLM-4-Flash |
| **数据库** | MySQL 8.0 + MyBatis-Plus 3.5.5 |
| **协议层** | JSON-RPC 2.0, MCP 协议 |
| **工具库** | Lombok, Hutool 5.8.23 |
| **构建工具** | Maven 3.x |
| **运行环境** | Java 17 |

## 项目架构

```
travel-agent/
├── travel-common/                    # 公共模块
│   ├── core/                         # 核心工具类
│   └── ratelimit/                    # 熔断限流服务
├── travel-mcp-protocol/              # MCP 协议定义
│   ├── dto/                          # 数据传输对象
│   ├── jsonrpc/                      # JSON-RPC 核心
│   ├── a2a/                          # Agent-to-Agent 协议
│   └── util/                         # 工具类
├── travel-mcp-client/                # MCP 客户端
│   ├── client/                       # 客户端核心实现
│   └── config/                       # 配置类
├── travel-mcp-server-weather/        # 天气服务 (8081)
├── travel-mcp-server-poi/           # POI 服务 (8082)
├── travel-mcp-server-meal/          # 餐饮服务 (8083)
├── travel-mcp-server-budget/        # 预算服务 (8084)
├── travel-module-agent-biz/          # Agent 业务模块
├── travel-module-itinerary-biz/      # 行程规划业务模块
├── travel-module-user-biz/          # 用户业务模块
└── travel-web/                      # Web 入口 (8080)
```

## 核心模块

### 1. MCP Server 集群

| 服务 | 端口 | 工具数 | 功能描述 |
|------|------|--------|----------|
| Weather | 8081 | 1 | 获取天气预报（7天） |
| POI | 8082 | 9 | 地理编码、POI搜索、路线规划 |
| Meal | 8083 | 1 | 餐饮搜索（委托 POI 服务） |
| Budget | 8084 | 1 | 旅行预算估算 |

### 2. MCP Client

统一管理所有 MCP Server 连接，提供工具调用接口。

### 3. Agent 业务模块

- **ChatSession 管理**: 会话创建、历史记录
- **Agent 核心服务**: 消息处理、工具调用编排
- **AI 集成**: 与智谱 GLM-4-Flash 对接

### 4. 行程规划模块

根据用户需求生成完整旅行计划，包含景点、餐饮、天气提醒等。

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- IDE: IntelliJ IDEA / VS Code

### 配置文件

在 `travel-web/src/main/resources/` 目录下创建 `application-local.yaml`:

```yaml
spring:
  ai:
    openai:
      api-key: your-zhipu-api-key

travel:
  weather:
    api-key: your-weather-api-key
  amap:
    api-key: your-amap-api-key
```

### 启动服务

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

### API 测试

```bash
# 健康检查
curl http://localhost:8080/api/travel/health

# 生成旅行规划
curl -X POST http://localhost:8080/api/travel/plan \
  -H "Content-Type: application/json" \
  -d '{"request": "帮我规划北京3日游"}'
```

## API 文档

详细接口文档请参考: [API.md](API.md)

## 设计文档

详细设计文档请参考: [DESIGN.md](DESIGN.md)

## 架构文档

详细架构文档请参考: [ARCHITECTURE.md](ARCHITECTURE.md)

## 部署文档

详细部署文档请参考: [DEPLOYMENT.md](DEPLOYMENT.md)

## 项目特性

- **模块化设计**: 各服务独立部署、扩展
- **MCP 协议**: 标准化的 AI 工具调用协议
- **熔断限流**: 保护外部 API 调用
- **响应式编程**: 基于 WebFlux 的非阻塞 IO
- **流式响应**: 支持 SSE 长任务实时推送

## License

MIT License
