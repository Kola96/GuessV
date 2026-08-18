# GuessV 开发路线图与进度追踪

> **本文档是异地异步开发的单一状态源。**
> 任何 Agent / 开发者接手时：先读 [AGENTS.md](../../AGENTS.md)，再读本文件，从当前里程碑的第一个未勾选任务继续。
> 最后更新：2026-08-18

---

## 一、当前状态（每次开发后必须更新）

| 项目 | 值 |
|------|-----|
| **当前阶段** | Phase 1（MVP） |
| **当前里程碑** | M1：后端骨架与数据导入 🚧 |
| **当前任务** | Task 1 ✅ 完成，下一步 Task 2（数据库基础） |
| **进行中分支** | main |
| **阻塞事项** | 无 |

**状态标记约定**：⬜ 未开始 ｜ 🚧 进行中 ｜ ✅ 已完成

---

## 二、Agent 工作流（每次开发会话必做）

### 会话开始时
1. 读 `AGENTS.md`（架构总纲）
2. 读本文件的「当前状态」
3. 打开当前里程碑的计划文件（`docs/plans/phase-1/mX-*.md`）
4. 从第一个 `- [ ]` 未勾选的任务继续执行

### 每完成一个任务后
1. 勾选计划文件中对应的 `- [x]`
2. 更新本文件「当前状态」表
3. `git commit` + `git push`（这是同步到另一台设备的唯一手段）

### 里程碑完成时
1. 将里程碑标记为 ✅
2. 为下一个里程碑编写详细计划文件（步骤级，参照 M1 的格式）
3. 更新本文件并提交

---

## 三、Phase 1 里程碑总览

| 里程碑 | 目标 | 状态 | 详细计划 |
|--------|------|------|----------|
| **M1** 后端骨架与数据导入 | Spring Boot 可运行，10014 条数据入库，搜索 API 可用 | 🚧 | [m1-backend-foundation.md](./phase-1/m1-backend-foundation.md) |
| **M2** 用户系统 | 昵称+#ID 注册、Token 鉴权、前端昵称弹窗 | ⬜ | 启动时编写 |
| **M3** 游戏核心 | 属性对比、每日模式、单人模式 API 全部可用 | ⬜ | 启动时编写 |
| **M4** 前端游戏界面 | 完整可玩的游戏 UI（React + Framer Motion） | ⬜ | 启动时编写 |
| **M5** 运营后台基础版 | 管理员登录、VTuber 管理、字段锁定 | ⬜ | 启动时编写 |

**依赖关系**：M1 → M2 → M3 → M4 → M5（严格串行，每个里程碑产出可独立验证的软件）

---

## 四、里程碑任务清单

### M1：后端骨架与数据导入

**目标**：`mvn spring-boot:run` 能启动，list.json 自动导入 SQLite，`/api/vtuber/search` 能搜到种子数据。

- [x] Task 1：Spring Boot 项目骨架（pom.xml、配置文件、统一响应体、全局异常、健康检查）
- [ ] Task 2：数据库基础（schema.sql、MyBatis-Plus 配置、SQLite 数据源）
- [ ] Task 3：VTuber 与团体实体 + Mapper（含 JSON TypeHandler）
- [ ] Task 4：其余实体（User / DailyTarget / GameRecord / PoolTag / OperationLog / Room / RoomPlayer）
- [ ] Task 5：list.json 数据导入器（空表时自动导入）
- [ ] Task 6：开发种子数据（10 位完整属性的 active VTuber）
- [ ] Task 7：VTuber 搜索 API
- [ ] Task 8：收尾验证与提交

### M2：用户系统

**目标**：新用户填写昵称即可游玩，凭证存 LocalStorage，API 鉴权可用。

- [ ] Task 1：昵称池与随机昵称 API（含敏感词过滤）
- [ ] Task 2：用户初始化 API（POST /api/user/init，昵称+#游戏ID，JWT）
- [ ] Task 3：鉴权拦截器（X-User-Token 解析与校验）
- [ ] Task 4：前端用户模块（昵称设置弹窗、LocalStorage 凭证、Axios 拦截器）
- [ ] Task 5：个人信息 API（GET /api/user/profile）

### M3：游戏核心

**目标**：每日模式和单人模式的全部游戏 API 可用，对比规则正确。

- [ ] Task 1：属性对比服务（7 维度对比规则，单测全覆盖）
- [ ] Task 2：每日目标定时任务（每日 00:00 UTC+8 刷新，排除近 30 天）
- [ ] Task 3：每日模式 API（GET /api/game/daily + POST /api/game/daily/guess）
- [ ] Task 4：单人模式 API（pools / start / guess / end，会话管理）
- [ ] Task 5：游戏记录持久化与断线恢复

### M4：前端游戏界面

**目标**：浏览器里完整可玩每日模式和单人模式。

- [ ] Task 1：React 项目骨架（Vite + Tailwind + Framer Motion + Zustand + Router）
- [ ] Task 2：API service 层 + TypeScript 类型定义
- [ ] Task 3：游戏布局（顶栏 / 状态栏 / 猜测记录区 / 输入区）
- [ ] Task 4：猜测输入框与自动补全
- [ ] Task 5：猜测卡片与属性对比逐行揭示动画
- [ ] Task 6：每日模式页面
- [ ] Task 7：单人模式页面（题库选择 + 无限重开）
- [ ] Task 8：胜负反馈（庆祝动画 / 答案揭晓）
- [ ] Task 9：响应式适配 + 打包进后端 static 联调

### M5：运营后台基础版

**目标**：管理员可登录后台，完成 VTuber 状态流转和属性编辑。

- [ ] Task 1：管理员账号与登录 API（BCrypt + JWT）
- [ ] Task 2：VTuber 管理 API（列表 / 详情 / 编辑 / 状态流转 / 字段锁定）
- [ ] Task 3：后台前端骨架（登录页 + 管理布局）
- [ ] Task 4：VTuber 列表页（状态筛选 / 搜索 / 分页）
- [ ] Task 5：VTuber 编辑页（锁定标识、保存自动锁定）
- [ ] Task 6：数据看板基础版（各状态数量统计）

---

## 五、Phase 2-4 概览（暂不细化）

| 阶段 | 内容 | 前置条件 |
|------|------|----------|
| Phase 2 | 爬虫定时任务、运营后台完善、更多属性、分享功能 | Phase 1 完成 |
| Phase 3 | WebSocket、房间系统、对战模式 | Phase 2 完成 |
| Phase 4 | 排行榜/天梯、账号绑定完善、多语言 | Phase 3 完成 |

---

*每个里程碑的详细计划在启动时编写，格式参照 [m1-backend-foundation.md](./phase-1/m1-backend-foundation.md)。*
