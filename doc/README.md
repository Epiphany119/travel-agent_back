# Travel Agent - 智能旅行规划系统

> 文档导航（2026-08-31）：请先阅读 [`API-CURRENT.md`](API-CURRENT.md)、[`ARCHITECTURE-CURRENT.md`](ARCHITECTURE-CURRENT.md) 和 [`REQUEST-FLOWS.md`](REQUEST-FLOWS.md)。本文件后面的 PR 演进记录是历史背景，当前端口、模块和接口以当前文档及源码为准。

## 当前入口

- 当前 API：[API-CURRENT.md](API-CURRENT.md)
- 当前架构：[ARCHITECTURE-CURRENT.md](ARCHITECTURE-CURRENT.md)
- 请求链路：[REQUEST-FLOWS.md](REQUEST-FLOWS.md)
- 部署补充：[DEPLOYMENT.md](DEPLOYMENT.md)

## 项目概述

Travel Agent 是一个基于 MCP (Model Context Protocol) 协议构建的智能旅行规划 AI Agent 系统。系统通过模块化架构提供天气查询、POI 搜索、餐饮推荐、预算估算、行程规划等功能。

---

## PR 演进历史

本项目经历了三个主要阶段的设计与实现：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     PR#1: 项目基础框架                                   │
│         Spring Boot + AI 集成 + 工具类 + 熔断限流                       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     PR#2: MCP Protocol 协议层                            │
│         JSON-RPC 2.0 + MCP 协议定义 + 统一通信规范                       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     PR#3: MCP Server 拆分实现                          │
│         4 个独立服务 + 领域驱动拆分 + 服务网格                           │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## PR#1: 项目基础框架

### 技术栈

| 分类 | 技术 |
|------|------|
| **核心框架** | Spring Boot 3.2.4, Spring WebFlux |
| **AI 能力** | Spring AI 1.0.0-M4, 智谱 GLM-4-Flash |
| **数据库** | MySQL 8.0 + MyBatis-Plus 3.5.5 |
| **工具库** | Lombok, Hutool 5.8.23 |
| **构建工具** | Maven 3.x |
| **运行环境** | Java 17 |

### 核心组件

#### 1. WeatherTool - 天气查询工具

```
WeatherTool
    │
    ├── 城市映射表 (44 个常用城市)
    ├── 拼音转换 (中文 → 英文拼音)
    ├── 天气描述翻译 (英文 → 中文)
    │       ├── 本地字典 (500+ 映射)
    │       └── LLM 翻译 (可选, 智谱)
    │
    └── API 调用
            ├── 和风天气 API (qweather)
            └── wttr.in (兜底)
```

**主要功能**：
- 支持 44 个常用中国城市
- 中英文双向转换
- 本地字典 + LLM 智能翻译
- 熔断限流保护

#### 2. PoiTool - 高德地图工具

```
PoiTool
    │
    ├── 地理编码 (geocode/regeo)
    ├── POI 搜索 (search/inputtips)
    ├── 路线规划
    │       ├── 步行 (walking)
    │       ├── 公交 (transit)
    │       ├── 驾车 (driving)
    │       └── 骑行 (bicycling)
    └── 距离测量 (distance)
```

**9 大功能**：
- 地址/坐标互转
- POI 关键词搜索
- 输入提示
- 4 种路线规划
- 距离测量

#### 3. RateLimitService - 熔断限流服务

```
RateLimitService
    │
    ├── 服务分组
    │       ├── weather (和风天气)
    │       └── amap (高德地图)
    │
    ├── 限流策略
    │       ├── weather: 800次/日
    │       └── amap: 4700次/日
    │
    └── 状态查询
            ├── getCurrentCount()
            ├── getLimit()
            └── getRemaining()
```

---

## PR#2: MCP Protocol 协议层

### 协议架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                         JSON-RPC 2.0 基础                            │
├─────────────────────────────────────────────────────────────────────┤
│  {                                                                 │
│    "jsonrpc": "2.0",           // 版本标识                          │
│    "id": "uuid-123",            // 请求标识                          │
│    "method": "tools/call",      // 方法名                            │
│    "params": { ... }            // 参数                              │
│  }                                                                 │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         MCP 协议扩展                                 │
├─────────────────────────────────────────────────────────────────────┤
│  Request:    tools/call      - 调用工具                             │
│              tools/list       - 列出工具                             │
│              resources/*      - 资源操作                             │
│                                                                     │
│  Response:   toolName         - 工具名称                            │
│              success          - 是否成功                             │
│              data             - 返回数据                            │
│              error            - 错误信息                             │
└─────────────────────────────────────────────────────────────────────┘
```

### 核心 DTO

| DTO | 职责 |
|-----|------|
| `JsonRpcRequest` | JSON-RPC 请求封装 |
| `JsonRpcResponse` | JSON-RPC 响应封装 |
| `JsonRpcError` | 错误信息封装 |
| `McpTool` | 工具定义 |
| `McpToolCall` | 工具调用请求 |
| `McpToolResult` | 工具调用结果 |
| `McpServerInfo` | 服务器信息 |

### 错误码定义

| 错误码 | 常量名 | 描述 |
|--------|--------|------|
| -32700 | PARSE_ERROR | 无效的 JSON |
| -32600 | INVALID_REQUEST | 无效的请求 |
| -32601 | METHOD_NOT_FOUND | 方法不存在 |
| -32602 | INVALID_PARAMS | 无效的参数 |
| -32603 | INTERNAL_ERROR | 内部错误 |
| -32000 | RATE_LIMIT_EXCEEDED | 超过限流 |
| -32001 | EXTERNAL_API_ERROR | 外部 API 错误 |

---

## PR#3: MCP Server 拆分实现

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    travel-agent-web (8080)                      │
│                    AI Agent 核心服务                             │
└─────────────────────────────────────────────────────────────────┘
              │              │              │              │
              ▼              ▼              ▼              ▼
     ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
     │   Weather    │ │     POI      │ │    Meal     │ │   Budget    │
     │  Server      │ │   Server     │ │   Server    │ │   Server    │
     │              │ │              │ │              │ │              │
     │  ┌────────┐ │ │ ┌─────────┐ │ │ ┌─────────┐ │ │             │
     │  │Weather │ │ │ │Geocode  │ │ │ │ Meal    │ │ │   预算      │
     │  │Service │ │ │ │Service  │ │ │ │Service  │ │ │   估算      │
     │  └────────┘ │ │ └─────────┘ │ │ │(委托POI)│ │ │   算法      │
     │              │ │ ┌─────────┐ │ │ └─────────┘ │ │             │
     │              │ │ │PoiSearch│ │ │              │ │             │
     │              │ │ │Service  │ │ │              │ │             │
     │              │ │ └─────────┘ │ │              │             │
     │              │ │ ┌─────────┐ │ │              │             │
     │              │ │ │ Route   │ │ │              │             │
     │              │ │ │Service  │ │ │              │             │
     │              │ │ └─────────┘ │ │              │             │
     │              │ │ ┌─────────┐ │ │              │             │
     │              │ │ │Distance │ │ │              │             │
     │              │ │ │Service  │ │ │              │             │
     │              │ │ └─────────┘ │ │              │             │
     └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
           8081              8082              8083              8084
```

### 各模块详解

#### 1. Weather Server (8081) — 天气服务

| 属性 | 值 |
|------|-----|
| 模块名 | `travel-mcp-server-weather` |
| 端口 | 8081 |
| 工具数 | 1 个 |

**提供的工具**：

| 工具名 | 功能 | 参数 |
|--------|------|------|
| `weather.get_forecast` | 获取 7 天天气预报 | `city`: 城市名称 |

---

#### 2. POI Server (8082) — 地点搜索服务

| 属性 | 值 |
|------|-----|
| 模块名 | `travel-mcp-server-poi` |
| 端口 | 8082 |
| 工具数 | 9 个 |
| Service 数 | 4 个 |

**4 个 Service 的职责划分**：

| Service | 工具数 | 工具列表 |
|---------|--------|----------|
| **GeocodeService** | 2 | `poi.geocode`, `poi.regeo` |
| **PoiSearchService** | 2 | `poi.search`, `poi.inputtips` |
| **RouteService** | 4 | `poi.route_walking`, `poi.route_transit`, `poi.route_driving`, `poi.route_bicycling` |
| **DistanceService** | 1 | `poi.distance` |

---

#### 3. Meal Server (8083) — 餐饮搜索服务

| 属性 | 值 |
|------|-----|
| 模块名 | `travel-mcp-server-meal` |
| 端口 | 8083 |
| 工具数 | 1 个 |

**特点**: 代理模式，内部委托 POI Server，不直接调高德 API

---

#### 4. Budget Server (8084) — 预算估算服务

| 属性 | 值 |
|------|-----|
| 模块名 | `travel-mcp-server-budget` |
| 端口 | 8084 |
| 工具数 | 1 个 |

**特点**: 纯内存计算，无需外部 API

**费用计算公式**:
```
总费用 = (200 + 天数 × 100) × 等级系数 × 城市系数
```

---

### 模块化设计的优势

| 优势 | 说明 |
|------|------|
| **独立部署** | 每个服务可单独启动/停止，不影响其他服务 |
| **独立扩展** | 热门服务（如 POI）可单独扩容 |
| **故障隔离** | 一个服务崩溃不影响整体 |
| **技术灵活** | 各服务可用不同技术栈优化 |
| **团队分工** | 不同团队负责不同服务 |
| **资源隔离** | 独立的端口、内存、线程池 |

---

## 项目架构

```
travel-agent/
├── travel-common/                    # PR#1: 公共模块
│   ├── core/                       # 核心工具类 (IdGenerator)
│   └── ratelimit/                  # 熔断限流服务
│
├── travel-mcp-protocol/            # PR#2: MCP 协议定义
│   ├── dto/                        # 数据传输对象
│   ├── jsonrpc/                    # JSON-RPC 核心
│   ├── a2a/                        # Agent-to-Agent 协议
│   └── util/                       # 工具类
│
├── travel-mcp-client/              # PR#2: MCP 客户端
│   ├── client/                     # 客户端核心实现
│   └── config/                     # 配置类
│
├── travel-mcp-server-weather/      # PR#3: 天气服务 (8081)
├── travel-mcp-server-poi/          # PR#3: POI 服务 (8082)
├── travel-mcp-server-meal/         # PR#3: 餐饮服务 (8083)
├── travel-mcp-server-budget/       # PR#3: 预算服务 (8084)
│
├── travel-module-agent-biz/          # PR#1: Agent 业务模块
│   ├── controller/                 # API 控制器
│   ├── service/                    # 服务层 (含 Tool 类)
│   ├── domain/                     # 领域模型
│   └── infra/                      # 基础设施
│
├── travel-module-itinerary-biz/     # PR#1: 行程规划业务模块
├── travel-module-user-biz/          # PR#1: 用户业务模块
│
└── travel-web/                     # PR#1: Web 入口 (8080)
```

---

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

---

## 相关文档

- [API.md](API.md) - 接口文档
- [DESIGN.md](DESIGN.md) - 详细设计
- [ARCHITECTURE.md](ARCHITECTURE.md) - 架构设计
- [DEPLOYMENT.md](DEPLOYMENT.md) - 部署文档

## 项目特性

- **模块化设计**: 各服务独立部署、扩展 (PR#3)
- **MCP 协议**: 标准化的 AI 工具调用协议 (PR#2)
- **熔断限流**: 保护外部 API 调用 (PR#1)
- **响应式编程**: 基于 WebFlux 的非阻塞 IO (PR#1)
- **AI 集成**: 智谱 GLM-4-Flash 无缝对接 (PR#1)

## License

MIT License
