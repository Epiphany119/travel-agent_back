# travel-agent 重构计划

> 起点：2026-08-12
> 范围：5 个 PR，按顺序执行，每 PR 后 `mvn clean install -DskipTests` 必须通过

---

## 已确认决策

| 项 | 选择 | 说明 |
|---|---|---|
| MCP 实现 | **Hybrid** | MCP server 自研 JSON-RPC over SSE；client 端可选 Spring AI MCP 注解 |
| A2A 通信 | **HTTP + SSE + JSON-RPC** | 同进程不同模块；后续拆微服务无需改协议 |
| 限流方案 | **Redis + 抽象接口**（PR#5 落地） | 现阶段保留本地文件实现，但接口抽离到 `common.ratelimit` |
| 智谱 Key | **application-local.yaml + .gitignore** | 主 yml 全部脱敏；local yml 本地存在但不进 git |
| 包名规范 | `com.travel.{module}.{layer}.*` | 旧 `com.travel.agent.*` 在 PR#5 全量 rename |

---

## 目标模块图（13 个 module）

```
travel-agent (pom parent)
├── travel-common               纯工具/枚举/Result  (无 Spring 依赖)
├── travel-web                  启动入口 + 聚合层 + SSE Controller
├── travel-mcp-protocol         [新] MCP JSON-RPC 协议 + A2A 协议 DTO
├── travel-mcp-client           [新] WebClient + SSE 客户端
├── travel-mcp-server-weather   [新] wttr.in 封装
├── travel-mcp-server-poi       [新] 高德 9 API 拆分 4 服务
├── travel-mcp-server-meal      [新] 餐饮推荐（起步：高德 POI categories=餐饮）
├── travel-mcp-server-budget    [新] 预算估算
├── travel-mcp-server-itinerary [新] 行程聚合（组合其他 4 个 server）
├── travel-a2a-runtime          [新] host_agent + 4 子 agent + orchestrator
├── travel-module-agent-biz     保留（chat session / 鉴权上下文）
├── travel-module-itinerary-biz 保留但解耦（改走 McpClient）
└── travel-module-user-biz      保留
```

---

## PR 拆分

### PR#1 清理 + 密钥脱敏（进行中）
- 智谱/高德/和风 Key → `application-local.yaml` + `.gitignore`
- `RateLimitException` 包路径修正
- `RateLimitService` 抽象成 pure java interface（实现仍可用本地文件）
- 删除 `SpringAIConfig`、`ToolConfig` 死代码
- `TravelWebApplication` 移除 bean debug；`@MapperScan` 收窄
- `GlobalExceptionHandler` 适配 `RateLimitException`

### PR#2 MCP 协议 + Client
- 新建 `travel-mcp-protocol`（DTO + JSON-RPC 编解码，零 Spring）
- 新建 `travel-mcp-client`（Spring WebFlux `WebClient` + SSE 订阅）
- echo server demo 验证

### PR#3 MCP server 拆分
- `WeatherTool` 实现 → `travel-mcp-server-weather`
- `PoiTool`（868 行）拆分到 `travel-mcp-server-poi`（Geocode/PoiSearch/Route/Distance 4 服务）
- 新建 `meal-mcp` / `budget-mcp`
- 旧路径保留 **facade**（不破 itinerary-biz 测试）

### PR#4 A2A 运行时
- 新建 `travel-a2a-runtime`
- `HostAgent` + `ItineraryOrchestratorAgent` + 4 子 agent
- `CompletableFuture.allOf` 阶段1（weather/poi/meal 并行）
- A2A SSE 端点 `/a2a/tasks/{taskId}/stream`
- 超时降级到 `dataWarnings[]`

### PR#5 SSE + 解耦 + 重命名
- `TravelPlanningApi` 增加 `/api/travel-plans/{planId}/stream`（`SseEmitter`）
- `itinerary-biz` 改走 `McpClient`，**删除 `import com.travel.agent.tool.*`**
- `com.travel.agent.*` 全量 rename 到 `com.travel.module.agent.biz.*`
- 删除 `TravelAgentController`（功能并入 itinerary-biz）
- 删除 `RateLimitController`
- application.yml 收敛到 `travel.mcp.{weather,poi}.*` 唯一来源

---

## 关键架构原则

1. **biz 模块互不依赖** — `itinerary-biz` 不再依赖 `agent-biz`；公共能力通过 `travel-mcp-client` 或 `travel-common` 共享
2. **MCP server 只提供事实数据** — 行程编排规则（天数、节奏、预算分配）属于 itinerary-biz 的领域服务
3. **子 agent 失败不能阻塞主流程** — 30s 超时降级到 `dataWarnings`，单点失败可恢复
4. **SSE 协议统一** — A2A StreamEvent 与前端 EventSource schema 一一对应，避免两层翻译

---

## 节奏（6 周）

- W1: PR#1 + PR#2
- W2-3: PR#3
- W4: PR#4
- W5: PR#5
- W6: 联调 + 性能压测 + 前端 streaming demo

---

## 验证指标

- PR#3 完成：4 个 MCP server 独立启动并通过 Postman 调通
- PR#4 完成：`curl -N /a2a/tasks/{id}/stream` 看到阶段1三子 agent 并行事件
- PR#5 完成：单次行程 p95 < 8s；首 token < 1.5s；前端 EventSource 拼出"打字机 + 工具进度 + 最终结构化 dayPlan"
