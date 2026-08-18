# GuessV - AGENTS.md

> 本文件是 GuessV 项目的架构总纲，定义核心决策和文档索引。
> 详细设计请查阅 `docs/` 目录下的专项文档。
> 最后更新：2026-08-18

---

## 一、项目概述

### 1.1 项目定位

GuessV 是一款受 Wordle 启发的猜 VTuber 游戏。玩家通过猜测 VTuber 的各项属性，在限定次数内推理出目标 VTuber。

- **中文名**：V一把
- **英文名**：GuessV
- **核心玩法**：每日一题 + 单人练习 + 多人对战（远期）

### 1.2 核心游戏循环

```
玩家输入 VTuber 名字
    ↓
系统返回该 VTuber 与目标 VTuber 的属性对比
    ↓
玩家根据提示缩小范围
    ↓
在限定次数内猜中 → 胜利
未猜中 → 失败，揭晓答案
```

### 1.3 游戏模式

| 模式 | 说明 | 状态 |
|------|------|------|
| 每日模式 | 服务端统一出题，全球玩家同题 | Phase 1 |
| 单人模式 | 随机抽V，可选题库，无限重开 | Phase 1 |
| 对战模式 | 多人联机竞速/回合制 | Phase 3（架构预留） |

---

## 二、技术架构决策

### 2.1 技术栈选型

| 层级 | 技术 | 版本 | 详细文档 |
|------|------|------|----------|
| 前端框架 | React | 18.x | [技术栈选型](./docs/architecture/001-tech-stack.md) |
| 构建工具 | Vite | 5.x | - |
| 样式方案 | Tailwind CSS | 3.x | - |
| 动效库 | Framer Motion | 11.x | - |
| 状态管理 | Zustand | 4.x | - |
| 后端框架 | Spring Boot | 3.2.x | [技术栈选型](./docs/architecture/001-tech-stack.md) |
| ORM | MyBatis-Plus | 3.5.x | - |
| 数据库（开发） | SQLite | 3.x | [数据库设计](./docs/database/001-schema-overview.md) |
| 数据库（生产） | MySQL | 8.0.x | - |
| 部署 | 单 Jar（内嵌前端静态资源） | - | [部署方案](./docs/architecture/004-deployment.md) |

**关键决策理由**：见 [技术栈选型文档](./docs/architecture/001-tech-stack.md)

### 2.2 项目结构

Monorepo 结构，前后端同仓库管理：

```
GuessV/
├── frontend/          # React 前端
├── backend/           # Spring Boot 后端
├── data/              # 数据文件
├── scripts/           # 构建/部署脚本
├── docs/              # 项目文档
└── AGENTS.md
```

**部署形态**：开发时前后端分离（Vite proxy），构建时前端产物打进后端 Jar，**生产环境只有一个 `guessv.jar` 进程**。

**详细目录规范**：见 [项目结构文档](./docs/architecture/002-project-structure.md)
**部署方案**：见 [部署方案文档](./docs/architecture/004-deployment.md)

---

## 三、用户体系设计

### 3.1 核心决策

| 决策点 | 方案 | 理由 |
|--------|------|------|
| 用户标识 | 昵称 + #游戏ID | 允许重名，社交友好 |
| 入门门槛 | 首次填写昵称即可玩 | 零摩擦，提高转化 |
| 昵称生成 | 用户输入 OR 随机池 | 降低输入成本 |
| 内容安全 | 敏感词过滤 | 合规要求 |
| 凭证存储 | LocalStorage | 单设备便捷 |
| 跨设备 | 绑定用户名密码 | 数据迁移 |

### 3.2 用户生命周期

```
首次访问 → 设置昵称（可随机）→ 匿名游玩 → 可选绑定账号 → 跨设备同步
```

**详细设计**：见 [用户系统设计文档](./docs/architecture/003-user-system.md)

---

## 四、数据架构

### 4.1 VTuber 数据状态机

```
raw（全量池）→ candidate（候选队列）→ active（正式池）→ verified（已核实）
```

| 状态 | 说明 | 可操作 |
|------|------|--------|
| raw | vtbs.moe 原始数据 | 运营挑选进候选 |
| candidate | 等待爬虫补全 | 爬虫补全数据 |
| active | 正式竞猜池 | 可用于游戏 |
| verified | 人工核实 | 最高优先级 |

### 4.2 数据优先级

| 优先级 | 来源 | 说明 |
|--------|------|------|
| 1（最高） | manual | 运营手动编辑，字段锁定 |
| 2 | admin | 管理员直接修改 |
| 3（最低） | crawler | 爬虫自动补全，可被覆盖 |

**详细表结构**：见 [数据库设计文档](./docs/database/001-schema-overview.md)

---

## 五、游戏模式架构

### 5.1 统一接口设计

所有游戏模式实现统一的 `GameSession` 接口：

```java
public interface GameSession {
    GuessResult guess(Long vtuberId);
    GameState getState();
    boolean isFinished();
    int getRemainingAttempts();
}
```

### 5.2 模式扩展

| 模式 | 接口 | 预留设计 |
|------|------|----------|
| 每日模式 | GameSession | 每日目标表、全服统一缓存 |
| 单人模式 | GameSession | 题库标签、会话管理 |
| 对战模式 | MultiGameSession | 房间表、WebSocket、玩家状态 |

**对战模式预留**：见 [对战模式表设计](./docs/database/005-multi-mode-tables.md)

---

## 六、API 架构

### 6.1 API 分层

| 层级 | 路径 | 说明 | 文档 |
|------|------|------|------|
| 游戏 API | `/api/game/**` | 每日/单人模式 | [游戏 API](./docs/api/001-game-api.md) |
| 用户 API | `/api/user/**` | 用户初始化/认证/统计 | [用户 API](./docs/api/002-user-api.md) |
| 管理 API | `/api/admin/**` | 运营后台 | [管理 API](./docs/api/003-admin-api.md) |

### 6.2 通用规范

- 统一响应格式：`{ code, message, data }`
- 用户凭证：`X-User-Token` Header
- 管理员凭证：`Authorization: Bearer {token}`

---

## 七、运营后台架构

### 7.1 功能模块

| 模块 | 功能 | 优先级 |
|------|------|--------|
| 数据看板 | 统计概览 | P0 |
| VTuber 管理 | 数据管理、状态流转 | P0 |
| 爬虫监控 | 爬虫状态、手动触发 | P1 |
| 每日目标 | 历史查看、手动调整 | P1 |

**详细设计**：见 [运营后台文档](./docs/admin/001-overview.md)

---

## 八、爬虫系统设计

### 8.1 架构决策

数据补全是轻量任务，**不做独立爬虫服务**，直接用 Spring Boot 定时任务实现：

```
@Scheduled 定时任务 → 萌娘百科 API（JSON）/ 官网页面（Jsoup）→ 合并入库
```

| 决策 | 方案 | 理由 |
|------|------|------|
| 实现方式 | Spring Boot `@Scheduled` | 零额外组件，部署简单 |
| HTTP 客户端 | OkHttp | 轻量可靠 |
| 数据获取 | 萌娘百科 MediaWiki API 优先 | 直接返回 JSON，无需解析 HTML |
| 兜底方案 | Jsoup 解析官网页面 | 补充官方数据 |
| 运行策略 | 每日凌晨 2:00，单线程串行 | 防封禁，量级可接受 |
| 锁定保护 | 合并时跳过 locked_fields | 人工数据不被覆盖 |

**详细设计**：见 [爬虫系统设计](./docs/crawler/001-architecture.md)

---

## 九、安全与防作弊

### 9.1 核心措施

| 措施 | 说明 |
|------|------|
| 服务端校验 | 每日目标、猜测结果均在服务端判定 |
| 前端不存答案 | 每日目标 ID 不下发到前端 |
| 请求频率限制 | 同一 IP 每分钟最多 60 次请求 |
| 会话隔离 | 每个游戏会话独立标识，无法推测 |

### 9.2 数据安全

| 措施 | 说明 |
|------|------|
| 密码加密 | BCrypt 哈希存储 |
| SQL 注入防护 | MyBatis-Plus 参数化查询 |
| 敏感词过滤 | 昵称、用户名内容安全 |

---

## 十、开发工作流程

### 10.1 Git 分支策略

| 分支 | 用途 |
|------|------|
| `main` | 生产环境，禁止直接提交 |
| `develop` | 开发主分支 |
| `feature/xxx` | 功能分支 |
| `fix/xxx` | 修复分支 |

### 10.2 提交信息规范

```
<type>(<scope>): <subject>
```

| 类型 | 说明 |
|------|------|
| feat | 新功能 |
| fix | 修复 bug |
| docs | 文档更新 |
| refactor | 重构 |

### 10.3 异地异步开发工作流

由于项目在公司和家里两头开发，Agent 上下文不共享。**所有开发上下文必须持久化在仓库里**，通过 git 同步。

**每次开发会话开始时：**
1. 读 `AGENTS.md`（本文档，架构总纲）
2. 读 [`docs/plans/000-roadmap.md`](./docs/plans/000-roadmap.md) 的「当前状态」表
3. 打开当前里程碑的计划文件（`docs/plans/phase-1/mX-*.md`）
4. 从第一个 `- [ ]` 未勾选的任务继续执行

**每完成一个任务后：**
1. 勾选计划文件中对应的 `- [x]`
2. 更新 `000-roadmap.md` 的「当前状态」表
3. `git commit` + `git push`（这是同步到另一台设备的唯一手段）

**里程碑完成时：**
1. 标记里程碑为 ✅
2. 为下一个里程碑编写详细计划文件（参照 M1 格式）
3. 更新路线图并提交

**详细路线图与进度**：见 [开发路线图](./docs/plans/000-roadmap.md)

---

## 十一、文档索引

### 架构设计
- [技术栈选型](./docs/architecture/001-tech-stack.md)
- [项目结构](./docs/architecture/002-project-structure.md)
- [用户系统设计](./docs/architecture/003-user-system.md)
- [部署方案](./docs/architecture/004-deployment.md)

### 数据库设计
- [数据库总览](./docs/database/001-schema-overview.md)
- [VTuber 表设计](./docs/database/002-vtuber-table.md)
- [用户表设计](./docs/database/003-user-table.md)
- [游戏表设计](./docs/database/004-game-tables.md)
- [对战模式表设计](./docs/database/005-multi-mode-tables.md)

### API 设计
- [游戏 API](./docs/api/001-game-api.md)
- [用户 API](./docs/api/002-user-api.md)
- [运营后台 API](./docs/api/003-admin-api.md)

### 游戏逻辑
- [每日模式设计](./docs/game/001-daily-mode.md)
- [单人模式设计](./docs/game/002-single-mode.md)
- [属性对比规则](./docs/game/003-comparison-rules.md)

### 其他
- [运营后台设计](./docs/admin/001-overview.md)
- [爬虫系统架构](./docs/crawler/001-architecture.md)

---

## 十二、路线图

### Phase 1：MVP（4-6 周）

- [ ] 数据库设计与初始化
- [ ] VTuber 数据导入（list.json → SQLite）
- [ ] 用户系统（昵称+#ID）
- [ ] 每日模式核心逻辑
- [ ] 单人模式核心逻辑
- [ ] 基础属性对比（7 个维度）
- [ ] 前端游戏界面
- [ ] 运营后台基础版

### Phase 2：数据完善（2-3 周）

- [ ] 爬虫服务开发
- [ ] 运营后台完善
- [ ] 更多属性维度加入
- [ ] 结果分享功能

### Phase 3：对战模式（4-6 周）

- [ ] WebSocket 实时通信
- [ ] 房间系统
- [ ] 竞速/回合两种对战规则

### Phase 4：社区与排名（持续）

- [ ] 排行榜/天梯
- [ ] 用户账号系统完善
- [ ] 多语言支持

---

## 附录

### A. 参考资源

| 资源 | 链接 |
|------|------|
| vtbs.moe 数据 | https://vdb.vtbs.moe/json/list.json |
| 萌娘百科 | https://zh.moegirl.org.cn |
| Hololive 官网 | https://hololive.hololivepro.com |

### B. 术语表

| 术语 | 说明 |
|------|------|
| VTuber | 虚拟主播，Virtual YouTuber |
| V | VTuber 的简称 |
| 大物 | 知名度很高的 VTuber |
| 个人势 | 独立运营的 VTuber |
| 企业势 | 隶属于公司的 VTuber |
| 毕业 | VTuber 停止活动/引退 |

---

*本文档是 GuessV 项目的架构总纲，详细设计请查阅 docs/ 目录下的专项文档。*
*如有冲突，以最新版本为准。*
