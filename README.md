# 🚀 智能旅行规划 Agent

基于 MCP (Model Context Protocol) + A2A (Agent-to-Agent) 架构的多 Agent 协作旅行规划系统。通过多个专业化 Agent 协作，智能完成旅行目的地规划、天气查询、地点推荐、餐饮搜索和预算估算。

## ✨ 功能特性

- 🔄 **多 Agent 协作** - Planner、Coordinator、Executor 三个 Agent 分工协作
- 📡 **实时流式响应** - SSE 流式输出，实时展示规划过程
- 🌤️ **天气查询** - 支持 7 天天气预报，覆盖 44 个常用城市
- 📍 **地点推荐** - 高德地图 POI 搜索，景点、地标全覆盖
- 🍜 **餐饮搜索** - 智能餐厅推荐，口味偏好匹配
- 💰 **预算估算** - 根据天数和目的地智能估算旅行费用
- 🗺️ **行程规划** - 自动生成每日行程安排
- 🔌 **MCP 协议** - 标准化的工具调用协议，易于扩展

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Frontend (React)                            │
│                          http://localhost:3000                           │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │ HTTP / SSE
┌───────────────────────────────▼─────────────────────────────────────────┐
│                     A2A Runtime (Spring AI)                             │
│                     http://localhost:8086                                │
│                                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │
│  │   Planner    │  │  Coordinator │  │   Executor   │  │    Host    │  │
│  │    Agent     │  │    Agent     │  │    Agent     │  │    Agent   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  └────────────┘  │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │ MCP Client (HTTP)
┌───────────────────────────────▼─────────────────────────────────────────┐
│                         MCP Server 集群                                   │
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │  Weather    │  │     POI     │  │    Meal     │  │   Budget    │   │
│  │   Server   │  │   Server    │  │   Server    │  │   Server    │   │
│  │   :8081    │  │   :8082     │  │   :8083     │  │   :8084     │   │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │
│                                                                             │
│  ┌─────────────┐                                                          │
│  │ Itinerary   │                                                          │
│  │   Server    │                                                          │
│  │   :8085     │                                                          │
│  └─────────────┘                                                          │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          ▼                     ▼                     ▼
    ┌──────────┐          ┌──────────┐          ┌──────────┐
    │ 高德天气  │          │ 高德地图  │          │ 餐饮搜索  │
    │   API    │          │   API    │          │   API    │
    └──────────┘          └──────────┘          └──────────┘
```

## 📦 微服务模块

| 模块 | 端口 | 技术栈 | 说明 |
|------|------|--------|------|
| travel-frontend | 3000 | React + Vite + TailwindCSS | 前端界面 |
| travel-a2a-runtime | 8086 | Spring Boot + Spring AI | A2A 运行时，核心编排 |
| travel-mcp-server-weather | 8081 | Spring Boot | 天气查询服务 |
| travel-mcp-server-poi | 8082 | Spring Boot | 地点搜索服务 |
| travel-mcp-server-meal | 8083 | Spring Boot | 餐饮推荐服务 |
| travel-mcp-server-budget | 8084 | Spring Boot | 预算估算服务 |
| travel-mcp-server-itinerary | 8085 | Spring Boot | 行程规划服务 |

## 🛠️ 技术栈

### 后端

- **核心框架**: Java 17, Spring Boot 3.2.4
- **AI 能力**: Spring AI 1.0.0-M4
- **LLM**: OpenAI GPT-4o-mini / 智谱 GLM-4
- **数据库**: MySQL 8.0 + MyBatis-Plus 3.5.5
- **工具库**: Lombok, Hutool 5.8.23
- **构建工具**: Maven 3.x

### 前端

- **框架**: React 18
- **语言**: TypeScript 5
- **构建工具**: Vite 5
- **样式**: TailwindCSS 3.4
- **Markdown**: react-markdown

### 协议

- **MCP**: Model Context Protocol - AI 工具调用标准
- **A2A**: Agent-to-Agent - 多 Agent 通信协议

## 📁 项目结构

```
travel-agent-back/
├── pom.xml                           # 父 POM
│
├── .env.example                      # 环境变量示例
│
├── travel-frontend/                  # 前端应用
│   ├── src/
│   │   ├── App.tsx                  # 主应用组件
│   │   ├── api/                     # API 调用
│   │   ├── components/              # React 组件
│   │   ├── hooks/                  # 自定义 Hooks
│   │   ├── types/                   # TypeScript 类型
│   │   ├── index.css               # 全局样式
│   │   └── main.tsx                # 入口文件
│   ├── package.json
│   └── vite.config.ts
│
├── travel-a2a-runtime/               # A2A 运行时核心
│   └── src/main/java/com/travel/a2a/
│       ├── A2aRuntimeApplication.java
│       ├── config/                   # 配置类
│       ├── controller/               # 控制器
│       ├── model/                    # 数据模型
│       └── service/                  # 服务层
│           ├── orchestrator/         # 编排器
│           └── subagent/             # 子 Agent
│
├── travel-common/                     # 公共模块
│   └── src/main/java/com/travel/common/
│       ├── core/                    # 核心工具类
│       └── ratelimit/               # 熔断限流
│
├── travel-mcp-protocol/              # MCP 协议定义
│   └── src/main/java/com/travel/mcp/
│       ├── dto/                     # 数据传输对象
│       ├── jsonrpc/                 # JSON-RPC 核心
│       ├── a2a/                     # A2A 协议
│       └── util/                    # 工具类
│
├── travel-mcp-client/                 # MCP 客户端
│   └── src/main/java/com/travel/mcp/
│       ├── client/                  # 客户端实现
│       └── config/                  # 配置类
│
├── travel-mcp-server-weather/         # 天气服务 (:8081)
├── travel-mcp-server-poi/           # POI 服务 (:8082)
├── travel-mcp-server-meal/           # 餐饮服务 (:8083)
├── travel-mcp-server-budget/          # 预算服务 (:8084)
│
├── travel-module-agent-biz/           # Agent 业务模块
│   └── src/main/java/com/travel/
│       ├── controller/
│       ├── service/
│       ├── domain/
│       └── infra/
│
├── travel-module-itinerary-biz/       # 行程规划模块
├── travel-module-user-biz/            # 用户模块
│
└── travel-web/                       # Web 入口 (:8080)
    └── src/main/java/com/travel/
        └── TravelWebApplication.java
```

## 🚀 快速开始

### 环境要求

| 组件 | 最低版本 | 推荐版本 |
|------|----------|----------|
| JDK | 17 | 17 LTS |
| Maven | 3.8 | 3.9+ |
| Node.js | 18 | 20 LTS |
| MySQL | 8.0 | 8.0.33+ |

### 方式一：手动启动

#### 1. 克隆项目

```bash
git clone <repository-url>
cd travel-agent-back
```

#### 2. 安装依赖

```bash
# 后端 - 编译项目
mvn clean install -DskipTests
```

#### 3. 配置环境变量

创建 `.env` 文件并填入 API Key：

```bash
cp .env.example .env
```

编辑 `.env` 文件：

```bash
# 编辑 .env
vim .env
```

```env
# API Keys
WEATHER_API_KEY=your_qweather_api_key
GAODE_API_KEY=your_amap_api_key
ZHIPU_API_KEY=your_zhipu_api_key
OPENAI_API_KEY=your_openai_api_key

# 数据库 (可选)
TRAVEL_DB_USERNAME=root
TRAVEL_DB_PASSWORD=your_password
```

**API Key 申请地址**：
- 和风天气: https://dev.qweather.com/
- 高德地图: https://lbs.amap.com/
- 智谱 AI: https://open.bigmodel.cn/

#### 4. 启动 MySQL (可选)

```bash
docker run -d \
  --name travel-mysql \
  -e MYSQL_ROOT_PASSWORD=your_password \
  -e MYSQL_DATABASE=travel_agent \
  -p 3306:3306 \
  mysql:8.0
```

#### 5. 启动 MCP Server 集群

在 5 个终端分别运行：

```bash
# 终端 1 - Weather Server
cd travel-mcp-server-weather && mvn spring-boot:run

# 终端 2 - POI Server
cd travel-mcp-server-poi && mvn spring-boot:run

# 终端 3 - Meal Server
cd travel-mcp-server-meal && mvn spring-boot:run

# 终端 4 - Budget Server
cd travel-mcp-server-budget && mvn spring-boot:run

# 终端 5 - Itinerary Server (可选)
cd travel-mcp-server-itinerary && mvn spring-boot:run
```

#### 6. 启动 A2A Runtime

```bash
cd travel-a2a-runtime && mvn spring-boot:run
```

#### 7. 启动前端

```bash
cd travel-frontend
npm install
npm run dev
```

访问 http://localhost:3000

### 方式二：前端单独开发

如果只想开发前端，可以连接远程后端：

```bash
cd travel-frontend

# 创建 .env 文件
echo "VITE_API_BASE_URL=http://your-backend-host:8086" > .env

npm install
npm run dev
```

## 🔧 环境变量配置

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| `OPENAI_API_KEY` | 是 | - | OpenAI API Key |
| `ZHIPU_API_KEY` | 否 | - | 智谱 API Key (备选) |
| `WEATHER_API_KEY` | 是 | - | 和风天气 API Key |
| `GAODE_API_KEY` | 是 | - | 高德地图 API Key |
| `TRAVEL_DB_USERNAME` | 否 | root | 数据库用户名 |
| `TRAVEL_DB_PASSWORD` | 否 | - | 数据库密码 |

### MCP Server URL 配置

| 服务 | 配置项 | 默认值 |
|------|--------|--------|
| Weather | `mcp.server.weather.url` | `http://localhost:8081` |
| POI | `mcp.server.poi.url` | `http://localhost:8082` |
| Meal | `mcp.server.meal.url` | `http://localhost:8083` |
| Budget | `mcp.server.budget.url` | `http://localhost:8084` |
| Itinerary | `mcp.server.itinerary.url` | `http://localhost:8085` |

## 📡 API 接口

### A2A Runtime API (8086)

#### SSE 流式规划

```http
GET /a2a/tasks/stream?request={user_request}
```

实时流式返回规划结果。

#### 异步规划任务

```http
POST /a2a/tasks
Content-Type: application/json

{
  "request": "帮我规划北京3日游，预算5000元"
}
```

### Web API (8080)

#### 旅行规划

```http
POST /api/travel/plan
Content-Type: application/json

{
  "request": "帮我规划北京3日游"
}
```

#### 查询天气

```http
GET /api/travel/weather?city=北京
```

#### 搜索 POI

```http
GET /api/travel/poi?keywords=故宫&city=北京
```

#### 健康检查

```http
GET /api/travel/health
```

### MCP Server API

#### 获取服务器信息

```http
GET /mcp/info
```

#### 调用工具

```http
POST /mcp/call
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "tools/call",
  "params": {
    "name": "weather.get_forecast",
    "arguments": {
      "city": "北京"
    }
  }
}
```

## 🛠️ 开发指南

### 添加新的 MCP Server

1. **创建模块目录**

```bash
mkdir travel-mcp-server-{name}
cd travel-mcp-server-{name}
```

2. **添加 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <parent>
        <groupId>com.travel</groupId>
        <artifactId>travel-agent</artifactId>
        <version>1.0.0</version>
    </parent>
    
    <artifactId>travel-mcp-server-{name}</artifactId>
    
    <properties>
        <java.version>17</java.version>
    </properties>
</project>
```

3. **实现 MCP Controller**

```java
@RestController
@RequestMapping("/mcp")
public class XxxMcpController {
    
    @GetMapping("/info")
    public JsonRpcResponse getServerInfo() {
        // 返回服务器信息和工具列表
    }
    
    @PostMapping("/call")
    public JsonRpcResponse callTool(@RequestBody JsonRpcRequest request) {
        // 处理工具调用
    }
}
```

4. **注册到 A2A Runtime**

在 `application.yml` 中添加：

```yaml
mcp:
  server:
    xxx:
      url: http://localhost:808X
```

### 添加新的 Agent 工具

1. **在对应的 MCP Server 中添加工具方法**

2. **定义工具描述**

```java
public static McpTool newTool() {
    return new McpTool(
        "namespace.tool_name",
        "工具描述 (AI 友好)",
        Map.of(
            "type", "object",
            "properties", Map.of(
                "param1", Map.of("type", "string", "description", "参数描述")
            ),
            "required", List.of("param1")
        )
    );
}
```

3. **实现工具逻辑**

### 调试技巧

#### 查看 MCP Server 日志

每个 MCP Server 启动时会输出工具列表：

```
Registered tools:
- weather.get_forecast
- ...
```

#### 限流状态查询

```bash
curl http://localhost:8080/api/ratelimit/status
```

#### 健康检查

```bash
# 检查所有服务
for port in 8080 8081 8082 8083 8084 8086; do
  echo -n ":$port ... "
  curl -s "http://localhost:$port/health" > /dev/null && echo "OK" || echo "FAILED"
done
```

## ❓ 常见问题

### 1. MCP Server 连接失败

**问题**: A2A Runtime 无法连接 MCP Server

**解决**:
1. 检查 MCP Server 是否正常启动: `lsof -i :8081`
2. 验证端口未被占用
3. 检查 `application.yml` 中的 URL 配置
4. 查看 MCP Server 日志排查具体错误

### 2. API Key 无效

**问题**: 返回 "API Key 无效" 或限流错误

**解决**:
1. 确认 API Key 正确配置在 `.env` 文件
2. 验证 API Key 是否过期或额度用完
3. 检查环境变量是否正确加载

### 3. 天气/POI 数据为空

**问题**: 查询返回空结果

**解决**:
1. 检查 API Key 权限和配额
2. 确认城市名称正确 (支持中文)
3. 查看具体 API 返回的错误信息

### 4. 前端无法连接后端

**问题**: 前端页面加载失败或无响应

**解决**:
1. 确认 A2A Runtime 已启动在 8086 端口
2. 检查前端 `.env` 中的 `VITE_API_BASE_URL`
3. 验证 CORS 配置是否正确

## 📚 相关文档

- [API 接口文档](doc/API.md) - 详细的 API 说明
- [架构设计文档](doc/ARCHITECTURE.md) - 系统架构详解
- [详细设计文档](doc/DESIGN.md) - 核心模块设计
- [部署文档](doc/DEPLOYMENT.md) - 生产环境部署

## 📄 License

MIT License
