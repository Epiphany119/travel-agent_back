# API 接口文档

## 目录

1. [Web API (8080)](#1-web-api-8080)
2. [MCP Protocol API](#2-mcp-protocol-api)
3. [PR#1: 工具类 API](#3-pr1-工具类-api)
4. [PR#2: MCP Server API](#4-pr2-mcp-server-api)
5. [PR#3: MCP Server 集群 API](#5-pr3-mcp-server-集群-api)
6. [公共数据结构](#6-公共数据结构)

---

## 1. Web API (8080)

### 1.1 旅行规划 API

#### 1.1.1 生成旅行行程

**请求**

```http
POST /api/travel/plan
Content-Type: application/json
```

**参数**

| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| request | string | 是 | 用户旅行需求描述 |

**请求示例**

```json
{
  "request": "帮我规划北京3日游，预算5000元，喜欢自然风光"
}
```

**响应**

```json
{
  "success": true,
  "data": "【北京3日旅行规划】\n\n📍 第一天：...\n..."
}
```

#### 1.1.2 查询天气

**请求**

```http
GET /api/travel/weather?city={city}
```

**响应示例**

```json
{
  "success": true,
  "data": {
    "city": "北京",
    "weatherList": [
      {
        "date": "2026-08-12",
        "textDay": "晴",
        "tempMin": "22",
        "tempMax": "32"
      }
    ]
  }
}
```

#### 1.1.3 搜索 POI

**请求**

```http
GET /api/travel/poi?keywords={keywords}&city={city}
```

#### 1.1.4 健康检查

**请求**

```http
GET /api/travel/health
```

---

### 1.2 Agent API

#### 1.2.1 创建会话

**请求**

```http
POST /api/agent/session
Content-Type: application/json
```

**参数**

| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| destination | string | 是 | 目的地 |
| startDate | string | 否 | 开始日期 (YYYY-MM-DD) |
| endDate | string | 否 | 结束日期 (YYYY-MM-DD) |
| budgetLevel | string | 否 | 预算等级 (economy/standard/luxury) |
| travelType | string | 否 | 旅行类型 |
| travelers | integer | 否 | 出行人数 |

**响应**

```json
{
  "sessionId": "session-abc123",
  "title": "去北京旅行规划",
  "status": "active",
  "createdAt": "2026-08-12T14:00:00"
}
```

#### 1.2.2 发送消息

**请求**

```http
POST /api/agent/message
Content-Type: application/json
```

**响应**

```json
{
  "messageId": "msg-xyz789",
  "role": "assistant",
  "content": "北京是一个历史悠久的城市，有很多值得一去的景点...",
  "needsToolCall": false
}
```

#### 1.2.3 获取消息历史

**请求**

```http
GET /api/agent/messages/{sessionId}
```

#### 1.2.4 获取用户会话列表

**请求**

```http
GET /api/agent/sessions/{userId}
```

#### 1.2.5 删除会话

**请求**

```http
DELETE /api/agent/session/{sessionId}
```

---

### 1.3 熔断限流 API

#### 1.3.1 获取熔断器状态

**请求**

```http
GET /api/ratelimit/status
```

**响应示例**

```json
{
  "weather": {
    "currentCount": 150,
    "limit": 800,
    "remaining": 650
  },
  "amap": {
    "currentCount": 800,
    "limit": 4700,
    "remaining": 3900
  }
}
```

---

## 2. MCP Protocol API

### 2.1 协议基础

所有 MCP 通信遵循 JSON-RPC 2.0 规范：

```json
{
  "jsonrpc": "2.0",
  "id": "uuid-123",
  "method": "tools/call",
  "params": {
    "name": "tool.name",
    "arguments": { ... }
  }
}
```

### 2.2 错误码

| 错误码 | 描述 |
|--------|------|
| -32700 | Parse error - 无效的 JSON |
| -32600 | Invalid Request - 无效的请求 |
| -32601 | Method not found - 方法不存在 |
| -32602 | Invalid params - 无效的参数 |
| -32603 | Internal error - 内部错误 |
| -32000 | Rate limit exceeded - 超过限流 |
| -32001 | External API error - 外部API错误 |

---

## 3. PR#1: 工具类 API

### 3.1 WeatherTool

#### WeatherRequest

| 字段 | 类型 | 描述 |
|------|------|------|
| city | string | 城市名称 |
| type | string | 类型 (默认: 7d) |

#### WeatherResponse

| 字段 | 类型 | 描述 |
|------|------|------|
| success | boolean | 是否成功 |
| city | string | 城市名称 |
| message | string | 错误信息 |
| weatherList | List\<DailyWeather\> | 天气预报列表 |
| remainingCalls | Integer | 剩余调用次数 |

#### DailyWeather

| 字段 | 类型 | 描述 |
|------|------|------|
| date | string | 日期 |
| tempMax | Integer | 最高温度 |
| tempMin | Integer | 最低温度 |
| textDay | string | 白天天气 |
| textNight | string | 夜间天气 |
| windDay | string | 白天风向 |
| windNight | string | 夜间风向 |
| humidity | Integer | 湿度 |
| precip | Double | 降水概率 |
| uvIndex | Integer | 紫外线指数 |

---

### 3.2 PoiTool

#### AmapRequest

| 字段 | 类型 | 描述 |
|------|------|------|
| action | string | 操作类型 |
| address | string | 地址 (geocode) |
| city | string | 城市 |
| location | string | 坐标 (regeo) |
| keywords | string | 关键词 (poi/inputtips) |
| types | string | POI 类型 |
| origin | string | 起点坐标 (route) |
| destination | string | 终点坐标 (route) |
| origins | string | 起点列表 (distance) |
| waypoints | string | 途经点 (driving) |
| offset | Integer | 每页数量 |
| page | Integer | 页码 |

**action 类型**:
- `geocode` - 地址转坐标
- `regeo` - 坐标转地址
- `poi` - POI 搜索
- `inputtips` - 输入提示
- `walking` - 步行路线
- `transit` - 公交路线
- `driving` - 驾车路线
- `bicycling` - 骑行路线
- `distance` - 距离测量

#### AmapResponse

| 字段 | 类型 | 描述 |
|------|------|------|
| success | boolean | 是否成功 |
| action | string | 操作类型 |
| message | string | 信息 |
| count | int | 结果数量 |
| total | int | 总数 |
| geocodeResult | GeocodeResult | 地理编码结果 |
| regeoResult | RegeoResult | 逆地理编码结果 |
| pois | List\<PoiInfo\> | POI 列表 |
| paths | List\<PathResult\> | 路线列表 |
| transits | List\<TransitResult\> | 公交路线列表 |
| distanceResults | List\<DistanceResult\> | 距离结果列表 |
| remainingCalls | Integer | 剩余调用次数 |

---

## 4. PR#2: MCP Server API

### 4.1 获取服务器信息

**请求**

```http
GET /mcp/info
```

**响应**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "name": "travel-mcp-server-weather",
    "version": "1.0.0",
    "tools": [
      {
        "name": "weather.get_forecast",
        "description": "获取天气预报（7天预报）",
        "inputSchema": {
          "type": "object",
          "properties": {
            "city": {
              "type": "string",
              "description": "城市名称"
            }
          },
          "required": ["city"]
        }
      }
    ]
  }
}
```

### 4.2 调用工具

**请求**

```http
POST /mcp/call
Content-Type: application/json
```

**请求体**

```json
{
  "jsonrpc": "2.0",
  "id": "uuid-123",
  "method": "tools/call",
  "params": {
    "name": "weather.get_forecast",
    "arguments": {
      "city": "北京"
    }
  }
}
```

**响应 (成功)**

```json
{
  "jsonrpc": "2.0",
  "id": "uuid-123",
  "result": {
    "toolName": "weather.get_forecast",
    "success": true,
    "data": { ... }
  }
}
```

---

## 5. PR#3: MCP Server 集群 API

### 5.1 Weather Server (8081)

#### 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/mcp/info` | 获取服务器信息和工具列表 |
| POST | `/mcp/call` | 调用工具 |
| GET | `/health` | 健康检查 |

#### 工具列表

##### weather.get_forecast

获取天气预报（7天预报）

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| city | string | 是 | 城市名称 |

---

### 5.2 POI Server (8082)

#### 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/mcp/info` | 获取服务器信息和工具列表 |
| POST | `/mcp/call` | 调用工具 |
| GET | `/health` | 健康检查 |

#### 工具列表 (9个)

##### 地理编码类

| 工具名 | 功能 | 必需参数 |
|--------|------|----------|
| `poi.geocode` | 地址转坐标 | address, city |
| `poi.regeo` | 坐标转地址 | location |

##### POI 搜索类

| 工具名 | 功能 | 必需参数 |
|--------|------|----------|
| `poi.search` | POI 关键词搜索 | keywords, city |
| `poi.inputtips` | 搜索建议 | keywords |

##### 路线规划类

| 工具名 | 功能 | 必需参数 |
|--------|------|----------|
| `poi.route_walking` | 步行路线规划 | origin, destination |
| `poi.route_transit` | 公交路线规划 | origin, destination, city |
| `poi.route_driving` | 驾车路线规划 | origin, destination |
| `poi.route_bicycling` | 骑行路线规划 | origin, destination |

##### 距离测量类

| 工具名 | 功能 | 必需参数 |
|--------|------|----------|
| `poi.distance` | 距离测量 | origins, destination |

---

### 5.3 Meal Server (8083)

#### 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/mcp/info` | 获取服务器信息和工具列表 |
| POST | `/mcp/call` | 调用工具 |
| GET | `/health` | 健康检查 |

#### 工具列表

##### meal.search

搜索餐厅/餐饮

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| keywords | string | 是 | 餐厅关键词 |
| city | string | 是 | 城市 |
| limit | number | 否 | 返回数量 |

---

### 5.4 Budget Server (8084)

#### 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/mcp/info` | 获取服务器信息和工具列表 |
| POST | `/mcp/call` | 调用工具 |
| GET | `/health` | 健康检查 |

#### 工具列表

##### budget.estimate

估算旅行预算

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| days | number | 是 | 旅行天数 |
| cities | array | 是 | 目的地城市列表 |
| level | string | 否 | 消费水平 |

**level 可选值**:
- `economy` - 经济型 (系数 0.7)
- `standard` - 标准型 (系数 1.0)
- `luxury` - 豪华型 (系数 2.0)

**响应示例**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "toolName": "budget.estimate",
    "success": true,
    "data": {
      "days": 5,
      "cities": ["北京"],
      "level": "standard",
      "cityCoefficient": 1.3,
      "baseCost": 700.0,
      "totalCost": 910.0,
      "currency": "CNY",
      "description": "根据您的5天行程，预计总费用约910.00元"
    }
  }
}
```

---

## 6. 公共数据结构

### 6.1 天气数据

| 字段 | 类型 | 描述 |
|------|------|------|
| city | string | 城市名称 |
| weatherList | array | 天气预报列表 |

### 6.2 POI 数据

| 字段 | 类型 | 描述 |
|------|------|------|
| name | string | 地点名称 |
| address | string | 地址 |
| location | string | 经纬度坐标 |
| type | string | 类型 |

### 6.3 路线数据

| 字段 | 类型 | 描述 |
|------|------|------|
| distance | string | 总距离 |
| duration | string | 预计时间 |
| steps | array | 路线步骤 |

### 6.4 预算估算

| 字段 | 类型 | 描述 |
|------|------|------|
| days | number | 天数 |
| cities | array | 城市列表 |
| level | string | 消费等级 |
| totalCost | number | 总费用 |
| currency | string | 货币单位 |
