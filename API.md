# Roamly 旅行智能体 · 后端接口文档

> 服务地址：`http://localhost:8080`（dev）
> 统一响应格式、鉴权方式、接口说明详见下文。
>
> 当前合约入口：[`doc/API-CURRENT.md`](doc/API-CURRENT.md)。本文保留扩展示例；如端点、端口、权限或返回结构与当前源码不一致，以当前合约和 Controller 为准。

---

## 1. 通用约定

### 1.1 统一响应结构

所有接口（除 SSE 流式、第三方 `Map` 直返外）统一返回：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | int | 状态码，见下表 |
| `message` | string | 提示信息 |
| `data` | object/array/null | 业务数据载体 |

**状态码约定**

| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 业务校验失败（参数错误 / 验证码错误等） |
| 401 | 未登录 / Token 失效 |
| 404 | 资源不存在 |
| 409 | 数据冲突（如邮箱已被占用） |
| 429 | 请求过于频繁（验证码 60s 内重复发送） |
| 500 | 服务器内部异常 |

### 1.2 鉴权方式

- **除** 注册、登录、发送验证码、健康检查外，多数接口需要鉴权。
- 前端在 `Authorization` 请求头携带 Token：

```
Authorization: Bearer <token>
```

- 登录/注册成功后返回的 `token` 即为此处的鉴权凭证。
- 401 时前端会自动清除本地 token 并跳转登录页。

> ⚠️ 注意：当前 `user` / `agent` 模块部分接口允许通过 `userId` 查询参数指定用户，缺少严格鉴权，属简化实现。

### 1.3 公共说明

- 请求/响应 `Content-Type` 均为 `application/json;charset=UTF-8`（上传类接口为 `multipart/form-data`）。
- 所有接口路径以 `/api` 开头（A2A 模块除外）。
- 前端 dev 通过 Vite 代理将 `/api`、`/a2a` 转发至 8080。

---

## 2. 认证模块（Auth）`/api/auth`

> 模块：`travel-module-auth-biz` · 控制器：`AuthController`

### 2.1 用户注册

- **POST** `/api/auth/register`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `username` | string | ✅ | 用户名（登录凭证） |
| `password` | string | ✅ | 密码 |
| `confirmPassword` | string | ✅ | 二次确认密码，需与 `password` 一致 |
| `email` | string | ❌ | 邮箱 |
| `emailCode` | string | ❌ | 邮箱验证码（填了 email 则必填） |

**响应 `data`**：`AuthTokenResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| `token` | string | JWT 鉴权令牌 |
| `userId` | string | 用户 ID |
| `username` | string | 用户名 |
| `expiresIn` | number | 有效期（秒） |

**成功示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { "token": "eyJhbGci...", "userId": "75476126", "username": "Kaoyanjuan", "expiresIn": 604800 }
}
```

**失败场景**：`400` 两次密码不一致；`409` 用户名/邮箱已被注册。

---

### 2.2 密码登录

- **POST** `/api/auth/login`

**请求体**

| 字段 | 类型 | 必填 |
|---|---|---|
| `username` | string | ✅ |
| `password` | string | ✅ |

**响应 `data`**：同 2.1 `AuthTokenResponse`。

**失败场景**：`401` 用户名或密码错误。

---

### 2.3 发送邮箱验证码

- **POST** `/api/auth/email/send-code`

**请求体**

| 字段 | 类型 | 必填 |
|---|---|---|
| `email` | string | ✅ |

**说明**：
- 向指定邮箱发送 6 位数字验证码，验证码存 Redis，**有效 5 分钟**。
- 同一邮箱 **60 秒内仅可发送一次**，重复发送返回 `429`。

**成功示例**

```json
{ "code": 200, "message": "操作成功", "data": null }
```

---

### 2.4 邮箱验证码登录

- **POST** `/api/auth/email/login`

**请求体**

| 字段 | 类型 | 必填 |
|---|---|---|
| `email` | string | ✅ |
| `code` | string | ✅ |

**说明**：
- 邮箱账号**已存在** → 校验验证码后直接登录。
- 邮箱账号**不存在** → **自动创建新账号**（用户名取邮箱），并返回新账号 Token。
- 邮箱存在但**未绑定验证身份**无法登录时，由业务侧返回对应提示。

**响应 `data`**：同 2.1 `AuthTokenResponse`。

**失败场景**：`400` 验证码错误或已过期。

---

### 2.5 绑定邮箱（需登录）

- **POST** `/api/auth/bind-email`

**请求头**：`Authorization: Bearer <token>`（必须）

**请求体**

| 字段 | 类型 | 必填 |
|---|---|---|
| `email` | string | ✅ |
| `code` | string | ✅ |

**说明**：
- 将邮箱绑定到当前登录账号，作为后续登录识别身份的依据。
- **先校验邮箱占用**（`409` 该邮箱已被其他账号绑定），**再校验验证码**。
- 校验通过后执行数据库更新，更新失败返回 `404`。

---

### 2.6 解绑邮箱（需登录）

- **POST** `/api/auth/unbind-email`

**请求头**：`Authorization: Bearer <token>`（必须）

**请求体**：无

**说明**：将当前账号的邮箱解绑。

---

### 2.7 登出

- **POST** `/api/auth/logout`

**请求头**：`Authorization: Bearer <token>`

**说明**：使当前 Token 失效（Redis 侧删除会话）。

---

### 2.8 校验 Token

- **GET** `/api/auth/verify`

**请求头**：`Authorization: Bearer <token>`

**响应示例**

```json
{ "code": 200, "message": "操作成功",
  "data": { "authenticated": true, "userId": "75476126" } }
```

未登录时：`"data": { "authenticated": false }`。

---

## 3. 用户模块（User）`/api/user`

> 模块：`travel-module-user-biz` · 控制器：`UserApi`
> 说明：多数接口通过 `userId` 查询参数指定用户，默认 `user_001`。

### 3.1 用户偏好 - 获取

- **GET** `/api/user/preferences`

| 参数 | 类型 | 必填 | 默认 |
|---|---|---|---|
| `userId` | string | ❌ | `user_001` |

**响应 `data`**（合并了 `auth_account` 邮箱信息，含偏好字段）关键字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | long | 偏好记录 ID |
| `userId` | string | 用户 ID |
| `name` / `username` | string | 用户名 |
| `email` | string | 绑定邮箱 |
| `favoriteDestinations` | string | 想去的目的地 |
| `preferredSeason` | string | 偏好季节 |
| `budgetLevel` | string | 预算档位 |
| `transportationPreference` | string | 交通偏好 |
| `travelStyle` | string | 旅行风格 |
| `dietaryRequirements` | string | 饮食要求 |
| ... | ... | 其余偏好字段 |

> 首次访问若用户无偏好记录，会**自动创建一条 default 偏好**。

---

### 3.2 用户偏好 - 保存

- **PUT** `/api/user/preferences`

**请求体**：`UserPreferencePO`（对象，只更新非 null 字段，不会用 null 覆盖已有数据）

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | string | 用户 ID |
| `name` | string | 昵称/用户名 |
| `email` | string | 邮箱 |
| `phone` | string | 手机号 |
| `favoriteDestinations` | string | 目的地 |
| ... | ... | 其它偏好字段同 3.1 |

**示例**

```json
{
  "userId": "75476126",
  "phone": "13800000000",
  "travelStyle": "深度体验"
}
```

---

### 3.3 昵称 - 获取

- **GET** `/api/user/nickname`（`userId` 查询参数）

**响应**

```json
{ "code": 200, "message": "操作成功", "data": { "nickname": "Kaoyanjuan" } }
```

### 3.4 昵称 - 更新

- **PUT** `/api/user/nickname`（`userId` 查询参数）

**请求体**

```json
{ "nickname": "新昵称" }
```

---

### 3.5 头像 - 上传

- **POST** `/api/user/avatar`（`multipart/form-data`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `file` | file | ✅ 头像图片 |
| `userId` | string | ❌ 默认 `user_001` |

**响应**：`{ "avatar": "/uploads/avatars/xxx.png" }`

### 3.6 头像 - 获取

- **GET** `/api/user/avatar`（`userId` 查询参数）

**响应**：`{ "avatar": "/uploads/...png" }`（无则空串）

---

### 3.7 灵感目的地 - 列表

- **GET** `/api/user/inspirations`（`userId` 查询参数）

**响应 `data`**：`InspirationPO[]`（含 `id`/`name`/`quote`/`description`/`imageUrl`/`bestSeason`/`estimatedBudget`/`tags`/`status`/`priority`/`sortOrder`）

### 3.8 灵感目的地 - 新增

- **POST** `/api/user/inspirations`

**请求体**：`InspirationPO`（`userId` 缺省则设为 `user_001`）

### 3.9 灵感目的地 - 更新

- **PUT** `/api/user/inspirations/{id}`

**请求体**：`InspirationPO`

### 3.10 灵感目的地 - 删除

- **DELETE** `/api/user/inspirations/{id}`

---

### 3.11 旅程 - 列表（含详情）

- **GET** `/api/user/journeys`（`userId` 查询参数）

**响应 `data`**：`JourneyDetailVO[]`

```json
[
  {
    "journey": { "id": 1, "destination": "西藏", "startDate": "2026-08-01", "totalDays": 7,
                 "travelType": "自驾", "totalCost": 12000, "rating": 5, "summary": "...",
                 "departureCity": "成都", "highlight": "...", "tips": "...", "weatherInfo": "...", "companions": "朋友" },
    "points": [ { "id": 1, "name": "布达拉宫", "latitude": 29.65, "longitude": 91.11, "visitDate": "2026-08-02", "description": "..." } ],
    "images": [ { "id": 1, "imageUrl": "...", "caption": "..." } ]
  }
]
```

### 3.12 旅程 - 详情

- **GET** `/api/user/journeys/{id}`

**响应**：`JourneyDetailVO`（结构同上）

### 3.13 旅程 - 新增

- **POST** `/api/user/journeys`

**请求体**：`JourneyPO`（`userId` 缺省则设为 `user_001`）

### 3.14 旅程 - 更新

- **PUT** `/api/user/journeys/{id}`

**请求体**：`JourneyPO`

### 3.15 旅程 - 删除

- **DELETE** `/api/user/journeys/{id}`

### 3.16 旅程 - 途经地点列表

- **GET** `/api/user/journeys/{id}/points` → `JourneyPointPO[]`

### 3.17 旅程 - 保存途经地点（整段覆盖）

- **POST** `/api/user/journeys/{id}/points`

**请求体**：`JourneyPointPO[]`（先删旧再插入）

### 3.18 旅程 - 图片列表

- **GET** `/api/user/journeys/{id}/images` → `JourneyImagePO[]`

### 3.19 旅程 - 保存图片（整段覆盖）

- **POST** `/api/user/journeys/{id}/images`

**请求体**：`JourneyImagePO[]`

---

### 3.20 旅行笔记 - 列表

- **GET** `/api/user/travel-notes`（`userId` 查询参数）→ `TravelNotePO[]`

### 3.21 旅行笔记 - 详情

- **GET** `/api/user/travel-notes/{id}` → `TravelNotePO`

### 3.22 旅行笔记 - 新增/更新

- **POST** `/api/user/travel-notes`

**请求体**：`TravelNotePO`（含 `title`/`destination`/`startDate`/`endDate`/`contentJson`/`shareToken`/`visibility` 等）

### 3.23 旅行笔记 - 复制

- **POST** `/api/user/travel-notes/{id}/copy`（`userId` 查询参数）

### 3.24 旅行笔记 - 分享查看

- **GET** `/api/user/travel-notes/share/{token}`

### 3.25 旅行笔记 - 删除

- **DELETE** `/api/user/travel-notes/{id}`

---

### 3.26 社区 - 公开笔记列表

- **GET** `/api/user/social/notes`（`page`/`size`，默认 0/20；可选 `q` 搜索标题/内容/目的地，`tag` 按标签筛选）

### 3.27 社区 - 公开笔记详情

- **GET** `/api/user/social/notes/{id}`

### 3.28 社区 - 笔记表态（点赞）

- **POST** `/api/user/social/notes/{id}/reaction`
  - `type`(query,🏃)、`userId`(query,默认 user_001)；重复点击可取消。

### 3.29 社区 - 评论列表

- **GET** `/api/user/social/notes/{id}/comments`

### 3.30 社区 - 发表评论

- **POST** `/api/user/social/notes/{id}/comments`
  - `userId`(query)；body：`{ "content": "..." }`

### 3.31 社区 - 发布笔记

- **POST** `/api/user/social/notes`

**请求体**

```json
{ "userId": "...", "title": "...", "content": "...", "coverUrl": "...", "destination": "杭州", "tags": ["人文", "慢旅行"] }
```

### 3.32 用户搜索

- **GET** `/api/user/users/search`（`q` 查询参数）

### 3.33 好友申请

- **POST** `/api/user/users/{id}/friend-request`
  - `from`(query)；body：`{ "message": "..." }`

### 3.34 地理编码（占位实现）

- **GET** `/api/user/geocode`（`address` 查询参数）→ `{ latitude, longitude, address }`（当前恒为 0）

### 3.35 通用图片上传

- **POST** `/api/user/upload`
  - `file`(multipart)、`category`(query，默认 general) → `{ "url": "..." }`

### 3.36 测试接口

- **GET** `/api/user/test` → `{ "message": "OK" }`

---

## 4. Agent 会话模块 `/api/agent`

> 模块：`travel-module-agent-biz` · 控制器：`AgentApi`

### 4.1 创建会话

- **POST** `/api/agent/sessions`

**请求体** `CreateSessionRequest`

| 字段 | 类型 | 必填 |
|---|---|---|
| `destination` | string | ✅ |
| `startDate` / `endDate` | string | ❌ |
| `budgetLevel` | string | ❌ |
| `travelType` | string | ❌ |
| `travelers` | number | ❌ |

**响应 `data`** `SessionResponse`：`{ sessionId, title, status, createdAt }`

### 4.2 发送消息

- **POST** `/api/agent/messages`

**请求体** `SendMessageRequest`：`{ sessionId, content }`

**响应 `data`** `MessageResponse`：`{ messageId, role, content, needsToolCall? }`

### 4.3 处理工具调用结果

- **POST** `/api/agent/tool-result`
  - `sessionId`(query)、`toolCallId`(query)、`toolName`(query)；body 为任意对象。

**响应**：`MessageResponse`

### 4.4 获取会话消息历史

- **GET** `/api/agent/sessions/{sessionId}/messages` → `MessageResponse[]`

### 4.5 获取用户会话列表

- **GET** `/api/agent/sessions`（`userId` 查询参数，默认 1）→ `SessionResponse[]`

### 4.6 删除会话

- **DELETE** `/api/agent/sessions/{sessionId}`

---

## 5. 问卷式行程 Agent `/api/agent/questionnaire`

> 控制器：`AgentQuestionnaireController`

### 5.1 创建问卷会话

- **POST** `/api/agent/questionnaire/start`

**请求体**（可选）：`{ "userId": "..." }`

**响应 `data`**：`QuestionnaireQuestion`

```json
{
  "sessionId": "xxxx",
  "stepIndex": 0,
  "totalSteps": 8,
  "question": "你打算去哪里玩？",
  "type": "destination",
  "options": ["海边", "城市", "山川", "境外"]
}
```

### 5.2 提交回答（SSE 流式）

- **POST** `/api/agent/questionnaire/{sessionId}/answer?step={n}&answer={text}`

**Content-Type**：`text/event-stream`

**SSE 事件**：

| 事件名 | 说明 |
|---|---|
| `parsed` | 解析用户回答 |
| `tool_call` | 触发工具调用（如天气/POI） |
| `tool_result` | 工具调用结果 |
| `next_question` | 下一道问题 |
| `plan` | 最终行程计划 |
| `error` | 出错 |

### 5.3 健康检查

- **GET** `/api/agent/questionnaire/health`

---

## 6. 行程生成模块 `/api/travel-plans`

> 模块：`travel-module-itinerary-biz` · 控制器：`TravelPlanningApi`

### 6.1 生成行程（同步）

- **POST** `/api/travel-plans/generate`

**请求体** `GenerateItineraryRequest`

| 字段 | 类型 | 必填 |
|---|---|---|
| `destination` | string | ✅ |
| `days` | number | ✅ |
| `budget` | number | ✅ |
| `travelers` | number | ❌ |
| `travelStyle` | string | ❌ |
| `interests` | string[] | ❌ |

**响应 `data`** `TravelPlanResponse`，结构：

```json
{
  "planId": "...", "destination": "北京", "days": 3, "totalBudget": 5000,
  "estimatedCost": 4200, "budgetStatus": "under_budget","overview": "...",
  "travelTips": ["..."], "packingList": ["..."],
  "dayPlans": [
    {
      "dayNumber": 1, "date": "2026-08-25", "theme": "...", "dayBudget": 1200,
      "transportation": "...", "notes": "...",
      "attractions": [ { "name": "...", "description": "...", "duration": 2, "ticketPrice": 60 } ],
      "meals": [ { "mealType": "午餐", "restaurantName": "...", "cuisine": "...", "avgPrice": 80, "reason": "..." } ]
    }
  ]
}
```

---

## 7. 旅行规划控制器 `/api/travel`

> 控制器：`TravelAgentController`

### 7.1 生成旅行行程

- **POST** `/api/travel/plan`

**请求体**：`{ "request": "..." }`

**响应**：

```json
{ "success": true, "message": "...", "data": "行程文本" }
```

### 7.2 查询天气

- **GET** `/api/travel/weather`（`city` 查询参数）

**响应**：`WeatherResponse`（城市、天气、温度等）

### 7.3 搜索 POI

- **GET** `/api/travel/poi`（`keywords` 查询参数，`city` 可选）

**响应**：`AmapResponse`（高德 POI 结果）

### 7.4 健康检查

- **GET** `/api/travel/health` → `{ "status": "UP", "service": "travel-agent" }`

---

## 8. POI 图片 `/api/poi`

> 控制器：`PoiImageController`

### 8.1 获取地点/餐厅图片

- **GET** `/api/poi/image?name={名称}&city={城市(可选)}`

**响应**：

```json
{
  "name": "西湖",
  "city": "杭州",
  "imageUrls": ["https://..." ]
}
```

---

## 9. A2A 任务模块 `/a2a/tasks`

> 模块：`travel-a2a-runtime` · 控制器：`A2aTaskController`
> 注意：此模块**没有 `/api` 前缀**。

### 9.1 创建并流式执行（用查询参数）

- **GET** `/a2a/tasks/stream`

**查询参数**

| 参数 | 类型 | 说明 |
|---|---|---|
| `destination` | string | 目的地 |
| `days` | int | 天数 |
| `budget` | number | 预算 |
| `travelers` | int | 人数 |
| `travelStyle` | string | 风格 |
| `interests` | string | 兴趣（逗号分隔） |

**Content-Type**：`text/event-stream`，回调按 `plan`/`done`/`error` 等事件推送。

### 9.2 创建任务（REST）

- **POST** `/a2a/tasks`

**请求体**：`TravelPlanRequest`

**响应**：`{ "taskId": "...", "status": "created" }`

### 9.3 查询任务状态

- **GET** `/a2a/tasks/{taskId}/status` → `{ "taskId": "...", "status": "processing" }`

### 9.4 获取任务 SSE 流

- **GET** `/a2a/tasks/{taskId}/stream`（body 可选）

---

## 10. MCP 工具模块

> 供 Agent 内部工具调用，多归一社区标准 MCP 协议接口，一般不对前端直接开放。

| 模块 | 控制器 | 说明 |
|---|---|---|
| `travel-mcp-server-budget` | `BudgetMcpController` | 预算工具 |
| `travel-mcp-server-meal` | `MealMcpController` | 餐饮/餐厅工具 |
| `travel-mcp-server-poi` | `PoiMcpController` | POI 搜索工具 |
| `travel-mcp-server-weather` | `WeatherMcpController` | 天气工具 |

---

## 附录 A：通用数据结构

### AuthTokenResponse
```json
{ "token": "string", "userId": "string", "username": "string", "expiresIn": "number" }
```

### 认证业务错误对照
| 场景 | code | message |
|---|---|---|
| 注册两次密码不一致 | 400 | 两次输入的密码不一致 |
| 邮箱已被绑定/占用 | 409 | 该邮箱已被其他账号绑定 |
| 验证码错误或过期 | 400 | 验证码错误或已过期 |
| 验证码发送频繁 | 429 | 验证码发送过于频繁，请稍后再试 |
| 未登录 | 401 | 请先登录 |

---

*本文档由项目后端各 Controller + 前端 api 层实际调用链梳理而成，与源码保持同步。*
