# GuessV · V一把

> 受 Wordle 启发的猜 VTuber 每日游戏。根据属性提示推理，在限定次数内猜出当天的目标 VTuber。

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 简介

GuessV 是一款猜 VTuber 的每日推理游戏。每天 00:00（UTC+8）系统会从 VTuber 池中随机选定一位作为当日目标，玩家通过输入 VTuber 名字进行猜测，每次猜测后会获得与目标 VTuber 的属性对比提示，逐步缩小范围，在 8 次机会内猜中即为胜利。

### 对比维度（10 个）

| 属性 | 对比方式 | 说明 |
|------|----------|------|
| 平台 | 交集匹配 | bilibili / YouTube / Twitter 等 |
| 团体 | 完全匹配 / 同公司 partial | Hololive / Nijisanji / VirtuaReal 等 |
| 出道年份 | 数值箭头 ↑↓ | 目标更早还是更晚 |
| 生日 | 同日 exact / 同月 partial | MM-DD 格式 |
| 性别 | 完全匹配 | 男 V 稀少，强排除 |
| 活动状态 | 活跃中 / 不活跃 | 还在直播就算活跃 |
| 发色 | 交集匹配 | 支持多色 |
| 语言 | 交集匹配 | 汉语 / 日语 / 英语，跨国 V 有区分度 |
| 粉丝量 | 数值箭头，10% 内 partial | 量级定位 |
| 名称 | 展示用 | 猜测的 V 名字 |

### 游戏模式

| 模式 | 说明 |
|------|------|
| 每日模式 | 全球同题，每日一题，8 次机会 |
| 单人模式 | 随机抽题，可选题库（日V/国V/英语圈/Hololive 等），无限重开 |
| 对战模式 | 多人联机（架构预留，Phase 3 实现） |

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | React + TypeScript | 18.x |
| 构建工具 | Vite | 5.x |
| 样式 | Tailwind CSS | 3.x |
| 动效 | Framer Motion | 11.x |
| 状态管理 | Zustand | 4.x |
| 后端框架 | Spring Boot | 3.2.x |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库（开发） | SQLite | 3.x |
| 数据库（生产） | MySQL | 8.0.x |
| 部署 | 单 Jar（内嵌前端静态资源） | - |

---

## 快速开始

### 环境要求

- **JDK 21+**
- **Maven 3.9+**
- **Node.js 18+**（仅前端开发需要）

### 开发模式（前后端分离）

```bash
# 终端 1：后端（端口 8080）
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 终端 2：前端（端口 5173，热更新）
cd frontend
npm install
npm run dev
```

浏览器访问 **http://localhost:5173**

### 单 Jar 集成模式

```bash
# 构建前端
cd frontend && npm run build

# 拷贝到后端静态资源目录
# Windows:
Remove-Item -Recurse -Force ../backend/src/main/resources/static -ErrorAction SilentlyContinue
Copy-Item -Recurse dist/* ../backend/src/main/resources/static/
# Linux:
rm -rf ../backend/src/main/resources/static && cp -r dist/* ../backend/src/main/resources/static/

# 启动后端（同时提供前端和 API）
cd ../backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

浏览器访问 **http://localhost:8080**

> 首次启动会自动导入 vtbs.moe 全量 VTuber 数据（约 9700+ 条）和 15 条种子数据。

---

## 运营后台

访问 `/admin` 路径进入运营后台。

- 默认账号：`admin`
- 默认密码：`admin123`
- 功能：数据看板、VTuber 列表管理、属性编辑（字段锁定）、状态流转

---

## 项目结构

```
GuessV/
├── frontend/          # React 前端
│   ├── src/
│   │   ├── components/     # 通用组件
│   │   ├── features/       # 业务模块（daily/single/admin）
│   │   ├── services/       # API 调用
│   │   ├── stores/         # Zustand 状态管理
│   │   └── types/          # TypeScript 类型
│   └── package.json
│
├── backend/           # Spring Boot 后端
│   ├── src/main/java/com/guessv/
│   │   ├── controller/     # REST API
│   │   ├── service/        # 业务逻辑
│   │   ├── mapper/         # MyBatis-Plus Mapper
│   │   ├── entity/         # 数据库实体
│   │   ├── config/         # 配置类
│   │   └── scheduler/      # 定时任务
│   └── pom.xml
│
├── data/              # 数据文件
│   └── list.json      # vtbs.moe 全量 VTuber 数据
│
├── docs/              # 项目文档
│   ├── architecture/  # 架构设计
│   ├── database/       # 数据库设计
│   ├── api/            # API 设计
│   ├── game/           # 游戏逻辑
│   └── plans/          # 开发计划与进度
│
├── AGENTS.md          # 架构总纲
└── README.md          # 本文件
```

---

## 数据来源

- VTuber 基础数据：[vtbs.moe](https://vdb.vtbs.moe/json/list.json)（10014 条，过滤后约 9700 条）
- 属性补全：萌娘百科（Phase 2 爬虫自动补全）
- 种子数据：15 位头部 VTuber（Hololive + VirtuaReal），手工整理

---

## 开发路线

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 1 | MVP（数据导入 + 用户系统 + 游戏核心 + 前端 + 运营后台） | ✅ |
| Phase 2 | 爬虫数据补全 + 分享功能 + 更多属性 | 规划中 |
| Phase 3 | 多人对战模式（WebSocket + 房间系统） | 远期 |
| Phase 4 | 排行榜 / 天梯 / 多语言 | 远期 |

详见 [开发路线图](./docs/plans/000-roadmap.md)。

---

## 致谢

- [vtbs.moe](https://vtbs.moe/) — VTuber 数据来源
- [萌娘百科](https://zh.moegirl.org.cn) — 属性数据来源
- [Wordle](https://www.nytimes.com/games/wordle) — 游戏灵感

---

## License

MIT
