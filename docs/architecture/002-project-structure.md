# 项目结构

> 本文档定义 GuessV Monorepo 的目录结构和模块划分。
> 返回 [AGENTS.md](../../AGENTS.md)

---

## 一、整体结构

```
GuessV/
├── frontend/                 # React 前端应用
│   ├── src/
│   │   ├── components/       # 通用 UI 组件（按钮、输入框、卡片等）
│   │   ├── features/         # 业务功能模块
│   │   │   ├── daily/        # 每日模式
│   │   │   ├── single/       # 单人模式
│   │   │   ├── multi/        # 对战模式（预留）
│   │   │   └── result/       # 结果展示/分享
│   │   ├── hooks/            # 自定义 React Hooks
│   │   ├── stores/           # Zustand 状态管理
│   │   ├── services/         # API 调用封装
│   │   ├── types/            # TypeScript 类型定义
│   │   ├── utils/            # 工具函数
│   │   ├── constants/        # 常量定义
│   │   └── styles/           # 全局样式
│   ├── public/               # 静态资源
│   ├── index.html
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   └── package.json
│
├── backend/                  # Spring Boot 后端应用
│   ├── src/main/java/com/guessv/
│   │   ├── GuessVApplication.java
│   │   ├── controller/       # REST API 控制器
│   │   │   ├── GameController.java
│   │   │   ├── UserController.java
│   │   │   ├── VtuberController.java
│   │   │   └── admin/        # 运营后台控制器
│   │   ├── service/          # 业务逻辑层
│   │   │   ├── GameService.java
│   │   │   ├── UserService.java
│   │   │   ├── VtuberService.java
│   │   │   └── impl/         # 实现类
│   │   ├── mapper/           # MyBatis-Plus Mapper 接口
│   │   ├── entity/           # 数据库实体类
│   │   ├── dto/              # 数据传输对象
│   │   │   ├── request/      # 请求 DTO
│   │   │   └── response/     # 响应 DTO
│   │   ├── config/           # 配置类
│   │   │   ├── MybatisPlusConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── CorsConfig.java
│   │   ├── scheduler/        # 定时任务
│   │   │   ├── DailyTargetScheduler.java
│   │   │   └── CrawlerScheduler.java
│   │   ├── service/crawler/  # 数据补全爬虫（含数据源 Fetcher）
│   │   ├── websocket/        # WebSocket（对战模式预留）
│   │   ├── exception/        # 全局异常处理
│   │   └── util/             # 工具类
│   ├── src/main/resources/
│   │   ├── application.yml           # 主配置
│   │   ├── application-dev.yml       # 开发环境（SQLite）
│   │   ├── application-prod.yml      # 生产环境（MySQL）
│   │   └── mapper/                   # MyBatis XML（复杂 SQL）
│   └── pom.xml
│
├── data/                     # 数据文件目录
│   ├── list.json             # vtbs.moe 原始数据（全量池）
│   └── guessv.db             # SQLite 数据库文件（开发环境）
│
├── docs/                     # 项目文档
│   ├── architecture/         # 架构设计文档
│   ├── database/             # 数据库设计文档
│   ├── api/                  # API 设计文档
│   ├── game/                 # 游戏逻辑设计文档
│   ├── admin/                # 运营后台设计文档
│   └── crawler/              # 爬虫设计文档
│
├── scripts/                  # 构建/部署脚本
│   ├── build.sh              # 一键构建（Linux/macOS）
│   └── build.ps1             # 一键构建（Windows）
│
├── .gitignore
├── README.md
└── AGENTS.md                 # 项目总纲（架构索引）
```

---

## 二、前端模块说明

### 2.1 components/ vs features/

| 目录 | 职责 | 示例 |
|------|------|------|
| `components/` | 纯 UI，无业务逻辑 | `Button.tsx`, `Input.tsx`, `Card.tsx` |
| `features/` | 业务功能，可包含 components | `daily/GuessInput.tsx`, `daily/ComparisonTable.tsx` |

### 2.2 stores/ 状态划分

| Store | 职责 | 持久化 |
|-------|------|--------|
| `useUserStore` | 用户信息、凭证 | LocalStorage |
| `useDailyGameStore` | 每日模式状态 | LocalStorage + 服务端同步 |
| `useSingleGameStore` | 单人模式状态 | 内存（可无限重开） |
| `useUiStore` | 界面状态（主题、语言） | LocalStorage |

### 2.3 services/ API 封装

```typescript
// services/api.ts - Axios 实例配置
// services/game.ts - 游戏相关 API
// services/user.ts - 用户相关 API
// services/admin.ts - 运营后台 API
```

---

## 三、后端模块说明

### 3.1 分层架构

```
Controller 层：接收请求、参数校验、返回响应
    ↓ 调用
Service 层：业务逻辑、事务控制
    ↓ 调用
Mapper 层：数据库操作（MyBatis-Plus）
    ↓ 映射
Entity 层：数据库实体
```

**铁律：禁止跨层调用**
- ❌ Controller 直接调用 Mapper
- ❌ Service 直接返回 Entity（必须转 DTO）

### 3.2 包结构规范

| 包 | 职责 | 示例 |
|----|------|------|
| `controller` | REST API | `GameController` |
| `service` | 业务接口 | `GameService` |
| `service.impl` | 业务实现 | `GameServiceImpl` |
| `mapper` | 数据访问 | `VtuberMapper` |
| `entity` | 数据库实体 | `Vtuber` |
| `dto.request` | 请求对象 | `GuessRequest` |
| `dto.response` | 响应对象 | `GuessResponse` |
| `config` | 配置类 | `SecurityConfig` |

---

## 四、环境配置管理

### 4.1 配置文件分离

| 文件 | 用途 | 包含敏感信息 |
|------|------|-------------|
| `application.yml` | 公共配置 | 否 |
| `application-dev.yml` | 开发环境 | 否（SQLite 路径） |
| `application-prod.yml` | 生产环境 | 是（从环境变量注入） |

### 4.2 环境变量约定

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `DB_URL` | 数据库连接 | `jdbc:mysql://localhost:3306/guessv` |
| `DB_USERNAME` | 数据库用户名 | `guessv_user` |
| `DB_PASSWORD` | 数据库密码 | `***` |
| `JWT_SECRET` | JWT 密钥 | `***` |
| `ADMIN_PASSWORD` | 管理员初始密码 | `***` |

---

## 五、构建与部署结构

### 5.1 开发环境

```bash
# 前端（端口 5173）
cd frontend && npm run dev

# 后端（端口 8080，含定时任务：每日目标刷新 + 爬虫）
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 5.2 生产环境（单 Jar）

```bash
# 构建：前端产物打进后端 Jar
./scripts/build.sh        # 产出 backend/target/guessv.jar

# 部署：上传 + 重启
scp backend/target/guessv.jar user@vps:/opt/guessv/
ssh user@vps "sudo systemctl restart guessv"
```

> 详细部署方案见 [部署方案文档](./004-deployment.md)

---

*文档版本：v1.0 | 更新日期：2026-08-18*
