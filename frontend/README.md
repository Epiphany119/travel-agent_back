# Travel Agent Frontend

Roamly 的 Vue 3 + Vite 前端，面向旅行规划、社区帖子、个人主页、笔记编辑器和本地源文件工作区。

## 开发

```bash
npm install
npm run dev -- --host 127.0.0.1
```

开发地址：`http://localhost:5173`。Vite 将 `/api`、`/a2a` 和 `/uploads` 代理到 `http://localhost:8080`；后端配置和接口以同级工程 [`travel-agent-back/API.md`](../travel-agent-back/API.md) 与 [`doc/API-CURRENT.md`](../travel-agent-back/doc/API-CURRENT.md) 为准。

## 页面入口

| 路由 | 用途 |
|---|---|
| `/explore` | 社区发现、旅行笔记卡片 |
| `/chat` | 快速表单和交互式 Agent 规划 |
| `/notes` | 数据库笔记、本地文件编辑、Markdown 排版/源码/协同 |
| `/profile` | 个人资料和公开帖子 |
| `/users/search`、`/users/:id` | 用户搜索和公开主页 |
| `/inspirations`、`/journeys` | 灵感目的地和历史旅程 |

## 编辑器数据边界

- 数据库笔记走 `src/api/note.ts` 的 `/api/notes`，可保存、分享和发布。
- 本地源文件走 `src/utils/editorWorkspace.ts`：文本快照在 localStorage，文件句柄在 IndexedDB，`Ctrl/Cmd + S` 可写回原文件。
- 不要把本地快照当成 `note_document` 内容，也不要在没有来源帖子 ID 的情况下建立社区复制关系。

## 验证

```bash
npm run build
```

需要登录后回归 `/explore`、`/chat`、`/notes`、`/profile` 和 `/users/search`。SSE 相关行为要同时确认加载、增量文字、外部数据告警、取消和重试状态。
