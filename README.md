# 智能旅行规划 Agent

基于 **Spring AI + 芋道 DDD 模块化架构** 的智能旅行规划系统。

## 项目结构

```
travel-agent-back/                  # 后端 - Maven多模块
├── pom.xml                         # 父POM
├── travel-common/                  # 通用模块
├── travel-module-agent-biz/        # Agent业务模块
├── travel-module-itinerary-biz/    # 行程模块
├── travel-module-user-biz/         # 用户模块
└── travel-web/                     # Web启动模块

travel-agent-front/                 # 前端 - Vue3 + Element Plus
└── src/
    ├── views/ChatView.vue
    ├── api/agent.ts
    └── router/
```

## 技术栈

### 后端
- Spring Boot 3.2.4
- Spring AI (智谱 GLM-4)
- Spring Cloud Alibaba
- MyBatis Plus
- H2 / MySQL
- 芋道 DDD 模块化架构

### 前端
- Vue 3.4
- TypeScript 5.4
- Element Plus
- Pinia
- Vite 5

## 快速开始

### 后端
```bash
cd travel-agent-back
mvn clean install
mvn spring-boot:run -pl travel-web
```

### 前端
```bash
cd travel-agent-front
npm install
npm run dev
```

访问: http://localhost:5173

## API 接口

- `POST /api/agent/sessions` - 创建会话
- `POST /api/agent/messages` - 发送消息
- `GET /api/agent/sessions/{sessionId}/messages` - 获取消息历史
- `GET /api/user/{userId}` - 获取用户信息

## 环境变量

```bash
export ZHIPU_API_KEY=your_zhipu_api_key
export AMAP_API_KEY=your_amap_api_key
```
