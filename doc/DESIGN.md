# 详细设计文档

## 目录

1. [PR#1: 项目基础框架设计](#1-pr1-项目基础框架设计)
2. [PR#2: MCP Protocol 协议设计](#2-pr2-mcp-protocol-协议设计)
3. [PR#3: MCP Server 拆分设计](#3-pr3-mcp-server-拆分设计)
4. [数据库设计](#4-数据库设计)
5. [工具实现](#5-工具实现)
6. [配置管理](#6-配置管理)

---

## 1. PR#1: 项目基础框架设计

### 1.1 模块架构

```
travel-agent/
├── travel-common/                    # 公共模块
│   ├── core/
│   │   └── util/IdGenerator.java    # ID 生成器
│   └── ratelimit/
│       └── RateLimitService.java    # 熔断限流服务
│
├── travel-module-agent-biz/          # Agent 业务模块
│   ├── controller/
│   │   ├── TravelAgentController.java    # 旅行规划 API
│   │   └── RateLimitController.java     # 熔断状态 API
│   ├── tool/
│   │   ├── WeatherTool.java         # 天气查询工具
│   │   └── PoiTool.java             # 高德地图工具
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── ChatSession.java     # 会话实体
│   │   │   ├── ChatMessage.java     # 消息实体
│   │   │   └── TravelPreference.java # 旅行偏好实体
│   │   ├── repository/
│   │   │   └── ChatSessionRepository.java
│   │   └── service/
│   │       └── AgentCoreService.java # Agent 核心服务
│   ├── application/
│   │   └── AgentApplicationService.java
│   └── infra/
│       ├── persistence/
│       │   ├── ChatSessionPO.java
│       │   ├── ChatMessagePO.java
│       │   └── ChatSessionMapper.java
│       └── repository/
│           └── ChatSessionRepositoryImpl.java
│
├── travel-module-itinerary-biz/      # 行程规划业务模块
│   └── application/
│       └── TravelPlanningService.java
│
├── travel-module-user-biz/          # 用户业务模块
│
└── travel-web/                     # Web 入口
```

### 1.2 WeatherTool 设计

#### 架构图

```
WeatherTool
    │
    ├── 输入验证层
    │       └── 限流检查 (RateLimitService)
    │
    ├── 城市映射层
    │       ├── CITY_LOCATION_MAP (中文城市 → ID)
    │       └── CITY_PINYIN_MAP (中文城市 → 拼音)
    │
    ├── API 调用层
    │       ├── 和风天气 API (qweather)
    │       ├── wttr.in (兜底)
    │       └── Geo API (城市 ID 查询)
    │
    ├── 翻译层
    │       ├── 本地字典 (WEATHER_DESC_ZH)
    │       │       ├── 精确匹配
    │       │       ├── 前缀匹配
    │       │       └── 子串匹配
    │       │
    │       └── LLM 翻译 (可选)
    │               └── 智谱 GLM-4-Flash
    │
    └── 输出层
            └── WeatherResponse
```

#### 核心常量

```java
// 城市映射 (44 个常用城市)
private static final Map<String, String> CITY_LOCATION_MAP = Map.of(
    "北京", "101010100",
    "上海", "101020100",
    "广州", "101280101",
    // ... 共 44 个城市
);

// 拼音映射
private static final Map<String, String> CITY_PINYIN_MAP = Map.of(
    "北京", "Beijing",
    "上海", "Shanghai",
    // ...
);
```

#### 天气描述翻译

```java
// 英文 → 中文 映射 (500+ 条)
private static final Map<String, String> WEATHER_DESC_ZH;

// 示例映射
m.put("Sunny", "晴天");
m.put("Light rain", "小雨");
m.put("Moderate rain", "中雨");
m.put("Heavy rain", "大雨");
m.put("Thundery outbreaks possible", "可能有雷暴");
```

### 1.3 PoiTool 设计

#### 架构图

```
PoiTool
    │
    ├── 输入验证层
    │       ├── API Key 检查
    │       └── 限流检查
    │
    ├── 操作路由层 (switch)
    │       ├── geocode     → doGeocode()
    │       ├── regeo      → doRegeo()
    │       ├── poi        → doPoiSearch()
    │       ├── inputtips  → doInputtips()
    │       ├── walking    → doWalking()
    │       ├── transit    → doTransit()
    │       ├── driving    → doDriving()
    │       ├── bicycling  → doBicycling()
    │       └── distance   → doDistance()
    │
    ├── API 调用层
    │       ├── 地理编码 API
    │       ├── POI 搜索 API
    │       ├── 路线规划 API
    │       └── 距离测量 API
    │
    └── 输出层
            └── AmapResponse
```

#### 9 大功能

| 操作 | 方法 | API |
|------|------|-----|
| geocode | 地址转坐标 | `/v3/geocode/geo` |
| regeo | 坐标转地址 | `/v3/geocode/regeo` |
| poi | POI 搜索 | `/v3/place/text` |
| inputtips | 输入提示 | `/v3/assistant/inputtips` |
| walking | 步行路线 | `/v3/direction/walking` |
| transit | 公交路线 | `/v3/direction/transit/integrated` |
| driving | 驾车路线 | `/v3/direction/driving` |
| bicycling | 骑行路线 | `/v3/direction/bicycling` |
| distance | 距离测量 | `/v3/distance` |

### 1.4 RateLimitService 设计

```java
@Service
public class RateLimitService {
    // 服务限额
    private static final Map<String, Integer> LIMITS = Map.of(
        "weather", 800,    // 和风天气: 800次/日
        "amap", 4700       // 高德地图: 4700次/日
    );

    // 计数器
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    // 尝试获取配额
    public boolean tryAcquire(String service) {
        int limit = LIMITS.getOrDefault(service, Integer.MAX_VALUE);
        AtomicInteger counter = counters.computeIfAbsent(service, k -> new AtomicInteger(0));
        
        if (counter.get() >= limit) {
            return false;  // 触发限流
        }
        counter.incrementAndGet();
        return true;
    }

    // 获取剩余配额
    public int getRemaining(String service) {
        int limit = LIMITS.getOrDefault(service, Integer.MAX_VALUE);
        int used = counters.getOrDefault(service, new AtomicInteger(0)).get();
        return Math.max(0, limit - used);
    }
}
```

### 1.5 AgentCoreService 设计

#### ReAct 模式实现

```java
@Service
public class AgentCoreService {
    /**
     * 处理用户消息
     */
    public String processMessage(ChatSession session, String userMessage, 
                                 Map<String, Object> toolResults) {
        // 1. 添加用户消息
        session.addMessage(ChatMessage.user(userMessage));

        // 2. 构建上下文
        String context = buildContext(session);

        // 3. 决定是否调用工具
        String toolCallDecision = decideToolCall(context);

        // 4. 如需调用工具，返回工具调用请求
        if (toolCallDecision != null) {
            return toolCallDecision;
        }

        // 5. 生成最终响应
        String response = generateResponse(session);
        session.addMessage(ChatMessage.assistant(response));

        return response;
    }

    /**
     * 处理工具结果
     */
    public String processToolResult(ChatSession session, String toolCallId, 
                                   String toolName, Object result) {
        session.addMessage(ChatMessage.tool(toolCallId, toolName, String.valueOf(result)));
        return generateResponse(session);
    }
}
```

---

## 2. PR#2: MCP Protocol 协议设计

### 2.1 协议分层

```
┌─────────────────────────────────────────────────────────────────┐
│                    MCP 应用层 (McpTool, McpServerInfo)            │
├─────────────────────────────────────────────────────────────────┤
│                    JSON-RPC 2.0 层                               │
│              (JsonRpcRequest, JsonRpcResponse)                   │
├─────────────────────────────────────────────────────────────────┤
│                    传输层 (McpTransport)                          │
│                        HTTP / WebSocket                          │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 JSON-RPC 请求/响应

#### JsonRpcRequest

```java
public record JsonRpcRequest(
    @JsonProperty("jsonrpc") String jsonrpc,  // "2.0"
    @JsonProperty("id") Object id,            // 请求 ID
    @JsonProperty("method") String method,    // 方法名
    @JsonProperty("params") Map<String, Object> params  // 参数
) {
    // 工厂方法
    public static JsonRpcRequest call(Object id, String method, Map<String, Object> params) {
        return new JsonRpcRequest("2.0", id, method, params);
    }
}
```

#### JsonRpcResponse

```java
public record JsonRpcResponse(
    @JsonProperty("jsonrpc") String jsonrpc,  // "2.0"
    @JsonProperty("id") Object id,            // 请求 ID
    @JsonProperty("result") Object result,    // 结果
    @JsonProperty("error") JsonRpcError error // 错误
) {
    // 成功响应
    public static JsonRpcResponse success(Object id, Object result) {
        return new JsonRpcResponse("2.0", id, result, null);
    }

    // 错误响应
    public static JsonRpcResponse error(Object id, JsonRpcError error) {
        return new JsonRpcResponse("2.0", id, null, error);
    }
}
```

### 2.3 MCP 工具定义

#### McpTool

```java
public record McpTool(
    @JsonProperty("name") String name,           // "namespace.method"
    @JsonProperty("description") String description,  // AI 友好描述
    @JsonProperty("inputSchema") Map<String, Object> inputSchema  // JSON Schema
) {
    // 示例
    public static McpTool weatherForecast() {
        return new McpTool(
            "weather.get_forecast",
            "获取天气预报（7天预报）",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "city", Map.of(
                        "type", "string",
                        "description", "城市名称"
                    )
                ),
                "required", List.of("city")
            )
        );
    }
}
```

#### McpToolCall

```java
public record McpToolCall(
    @JsonProperty("id") String id,           // 调用 ID
    @JsonProperty("name") String name,       // 工具名
    @JsonProperty("arguments") Map<String, Object> arguments  // 参数
) {}
```

#### McpToolResult

```java
public record McpToolResult(
    @JsonProperty("toolName") String toolName,
    @JsonProperty("success") boolean success,
    @JsonProperty("data") Object data,       // 成功时返回
    @JsonProperty("error") String error       // 失败时返回
) {}
```

### 2.4 McpTransport 设计

```java
@Component
public class McpTransport {
    private final WebClient webClient;

    // 工具调用
    public McpToolResult callTool(String serverUrl, McpToolCall toolCall) {
        JsonRpcRequest request = JsonRpcRequest.call(
            UUID.randomUUID().toString(),
            "tools/call",
            Map.of("name", toolCall.name(), "arguments", toolCall.arguments())
        );

        JsonRpcResponse response = webClient.post()
            .uri(serverUrl + "/mcp/call")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(JsonRpcResponse.class)
            .block();

        return parseResult(response);
    }

    // 获取服务器信息
    public McpServerInfo getServerInfo(String serverUrl) {
        JsonRpcResponse response = webClient.get()
            .uri(serverUrl + "/mcp/info")
            .retrieve()
            .bodyToMono(JsonRpcResponse.class)
            .block();

        return parseServerInfo(response);
    }

    // SSE 订阅
    public Flux<A2AStreamEvent> subscribeSse(String serverUrl, String taskId) {
        return webClient.get()
            .uri(serverUrl + "/mcp/stream/" + taskId)
            .retrieve()
            .bodyToFlux(A2AStreamEvent.class);
    }
}
```

---

## 3. PR#3: MCP Server 拆分设计

### 3.1 拆分方案

将原来单一模块中的功能拆分为 **4 个独立的 Spring Boot 服务**：

| 服务 | 端口 | 工具数 | 外部依赖 |
|------|------|--------|----------|
| Weather Server | 8081 | 1 | 和风天气 API |
| POI Server | 8082 | 9 | 高德地图 API |
| Meal Server | 8083 | 1 | 委托 POI Server |
| Budget Server | 8084 | 1 | 无 |

### 3.2 拆分原则

| 原则 | 说明 |
|------|------|
| **领域驱动** | 每个服务专注一个业务领域 |
| **独立部署** | 服务间无代码依赖，通过 HTTP/MCP 通信 |
| **外部依赖分离** | 外部 API 调用集中在独立服务 |
| **功能内聚** | 相关功能放在同一服务 |

### 3.3 POI Server 内部拆分 (4 Service)

```
PoiMcpController
       │
       ├── GeocodeService
       │      └── poi.geocode, poi.regeo
       │              └── 高德地理编码 API
       │
       ├── PoiSearchService
       │      └── poi.search, poi.inputtips
       │              └── 高德 POI API
       │
       ├── RouteService
       │      └── poi.route_walking
       │      └── poi.route_transit
       │      └── poi.route_driving
       │      └── poi.route_bicycling
       │              └── 高德路径规划 API
       │
       └── DistanceService
              └── poi.distance
                      └── 高德距离 API
```

### 3.4 Meal Server 代理模式

```
用户调用                    Meal Server                    POI Server
   │                           │                              │
   │ meal.search              │                              │
   │──────────────────────────>│                              │
   │                          │ poi.search (types=餐饮)     │
   │                          │─────────────────────────────>│
   │                          │                              │
   │                          │    POI 结果                   │
   │                          │<─────────────────────────────│
   │                          │                              │
   │    餐厅列表               │                              │
   │<──────────────────────────│                              │
   │                          │                              │
```

### 3.5 Budget Server 算法设计

```java
@Service
public class BudgetService {
    private static final double BASE_COST_PER_DAY = 200.0;
    private static final double DAILY_COST = 100.0;

    private static final Map<String, Double> LEVEL_MULTIPLIERS = Map.of(
        "economy", 0.7,
        "standard", 1.0,
        "luxury", 2.0
    );

    public BudgetEstimate estimate(int days, List<String> cities, String level) {
        double levelMultiplier = LEVEL_MULTIPLIERS.get(level);
        double cityCoefficient = calculateCityCoefficient(cities);
        double totalCost = (BASE_COST_PER_DAY + days * DAILY_COST)
                          * levelMultiplier * cityCoefficient;
        return BudgetEstimate.success(days, cities, level, cityCoefficient, totalCost);
    }
}
```

---

## 4. 数据库设计

### 4.1 ER 图

```
┌─────────────────┐     ┌─────────────────┐
│   chat_session  │     │  chat_message   │
├─────────────────┤     ├─────────────────┤
│ id (PK)         │────<│ session_id (FK)│
│ session_id      │     │ id (PK)         │
│ user_id         │     │ message_id      │
│ title           │     │ role            │
│ status          │     │ content         │
│ created_at      │     │ tool_call_id    │
│ last_active_at  │     │ created_at      │
└────────┬────────┘     └─────────────────┘
         │
         │ 1:1
         ▼
┌─────────────────┐
│ travel_preference│
├─────────────────┤
│ session_id (PK) │
│ destination     │
│ start_date      │
│ end_date        │
│ budget_level    │
│ travel_type     │
│ travelers       │
└─────────────────┘
```

### 4.2 表结构

#### chat_session

| 字段 | 类型 | 描述 |
|------|------|------|
| id | BIGINT | 主键 |
| session_id | VARCHAR(64) | 会话唯一标识 |
| user_id | BIGINT | 用户ID |
| title | VARCHAR(255) | 会话标题 |
| status | VARCHAR(20) | 状态: active/closed |
| created_at | DATETIME | 创建时间 |
| last_active_at | DATETIME | 最后活跃时间 |

#### chat_message

| 字段 | 类型 | 描述 |
|------|------|------|
| id | BIGINT | 主键 |
| session_id | VARCHAR(64) | 会话ID (外键) |
| message_id | VARCHAR(64) | 消息ID |
| role | VARCHAR(20) | 角色: user/assistant/system/tool |
| content | TEXT | 消息内容 |
| tool_call_id | VARCHAR(64) | 工具调用ID |
| created_at | DATETIME | 创建时间 |

#### travel_preference

| 字段 | 类型 | 描述 |
|------|------|------|
| session_id | VARCHAR(64) | 会话ID (主键) |
| destination | VARCHAR(100) | 目的地 |
| start_date | DATE | 开始日期 |
| end_date | DATE | 结束日期 |
| budget_level | VARCHAR(20) | 预算等级 |
| travel_type | VARCHAR(50) | 旅行类型 |
| travelers | INT | 出行人数 |

---

## 5. 工具实现

### 5.1 工具定义结构

```java
public record McpTool(
    String name,           // 工具名: namespace.method
    String description,    // AI 友好的描述
    Map<String, Object> inputSchema  // JSON Schema
)
```

### 5.2 工具命名规范

- 格式: `{namespace}.{method}`
- 命名空间: 服务名简写 (weather, poi, meal, budget)
- 方法名: 动词 + 名词 (get_forecast, search, estimate)

### 5.3 工具调用流程

```
AI Agent                      McpClient                  McpServer
    │                            │                          │
    │ callTool(toolCall)         │                          │
    │────────────────────────────>│                          │
    │                            │                          │
    │                            │ HTTP POST /mcp/call       │
    │                            │───────────────────────────>│
    │                            │                          │
    │                            │     JSON-RPC Response     │
    │                            │<──────────────────────────│
    │                            │                          │
    │   McpToolResult             │                          │
    │<─────────────────────────────│                          │
    │                            │                          │
```

---

## 6. 配置管理

### 6.1 多环境配置

```
application.yml          # 主配置 (默认)
application-dev.yml      # 开发环境
application-prod.yml     # 生产环境
application-local.yml    # 本地配置 (gitignore)
.env                     # 环境变量
```

### 6.2 MCP Server URL 配置

| 服务 | 配置项 | 默认值 |
|------|--------|--------|
| Weather | `travel.mcp.weather-url` | `http://localhost:8081` |
| POI | `travel.mcp.poi-url` | `http://localhost:8082` |
| Meal | `travel.mcp.meal-url` | `http://localhost:8083` |
| Budget | `travel.mcp.budget-url` | `http://localhost:8084` |

### 6.3 敏感信息管理

```bash
# .env 文件示例
ZHIPU_API_KEY=your-zhipu-key
WEATHER_API_KEY=your-weather-key
GAODE_API_KEY=your-gaode-key
TRAVEL_DB_PASSWORD=your-db-password
```

### 6.4 限流配置

| 服务 | 配置项 | 默认值 |
|------|--------|--------|
| weather | `qweather.daily-limit` | 800 |
| amap | `gaode.daily-limit` | 4700 |
