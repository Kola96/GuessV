# 技术栈选型

> 本文档记录 GuessV 项目的技术栈决策及理由。
> 返回 [AGENTS.md](../../AGENTS.md)

---

## 一、技术栈总览

| 层级 | 技术 | 版本 | 选型理由 |
|------|------|------|----------|
| 前端框架 | React | 18.x | 生态最成熟，组件丰富，Framer Motion 动画支持好 |
| 构建工具 | Vite | 5.x | 极速热更新，开箱即用 |
| 样式方案 | Tailwind CSS | 3.x | 原子化 CSS，快速开发，设计系统统一 |
| 动效库 | Framer Motion | 11.x | 声明式动画，React 原生支持，适合 UI 状态动画 |
| 状态管理 | Zustand | 4.x | 轻量（~1KB），无样板代码，TypeScript 友好 |
| 后端框架 | Spring Boot | 3.2.x | Java 生态成熟，部署简单（fat jar），团队熟悉 |
| ORM | MyBatis-Plus | 3.5.x | 国产优秀 ORM，CRUD 极简，SQL 可控性强 |
| 数据库（开发） | SQLite | 3.x | 零配置，单文件，适合快速迭代 |
| 数据库（生产） | MySQL | 8.0.x | 成熟稳定，社区支持好，运维成本低 |
| 部署 | 单 Jar（内嵌前端静态资源） | - | 生产环境仅一个进程，详见 [部署方案](./004-deployment.md) |

---

## 二、关键决策说明

### 2.1 前端：React vs Vue vs Svelte

**选择：React**

| 对比项 | React | Vue 3 | Svelte |
|--------|-------|-------|--------|
| 学习曲线 | 中等 | 低 | 低 |
| 生态丰富度 | ★★★★★ | ★★★★☆ | ★★★☆☆ |
| 动画库支持 | Framer Motion 最成熟 | GSAP 可用 | 需自行封装 |
| 团队熟悉度 | 高 | 中 | 低 |
| 就业市场 | 最大 | 大 | 小 |

**决策理由：**
- GuessV 核心动画是「猜测卡片逐行揭示」「匹配高亮」「全屏庆祝」，Framer Motion 的 `AnimatePresence` 和 `layout` 动画几乎为此场景量身定做
- React 生态遇到问题好查，招聘/协作容易

### 2.2 后端：Spring Boot vs FastAPI

**选择：Spring Boot**

| 对比项 | Spring Boot | FastAPI |
|--------|-------------|---------|
| 开发速度 | 中等 | 快 |
| 部署复杂度 | 低（fat jar） | 中（虚拟环境+WSGI） |
| 团队熟悉度 | 高（Java 背景） | 中 |
| 生态成熟度 | ★★★★★ | ★★★★☆ |
| 性能 | 高（JIT） | 中高 |

**决策理由：**
- 单 VPS 部署场景，Spring Boot 的 `java -jar` 远比 Python 的虚拟环境+Gunicorn+Nginx 省心
- Java 的强类型和编译期检查在重构时更安全
- 团队 Java 经验更丰富

### 2.3 数据库：SQLite → MySQL 兼容策略

**开发期用 SQLite，生产期无缝切换 MySQL**

实现方式：
1. 统一使用 MyBatis-Plus 标准 CRUD，禁止手写数据库方言
2. 实体类字段用 Java 标准类型（`LocalDateTime` 而非 `Date`）
3. 分页用 MyBatis-Plus 分页插件，禁止 `LIMIT`
4. 通过 `application-{env}.yml` 切换数据源

### 2.4 ORM：MyBatis-Plus vs JPA/Hibernate

**选择：MyBatis-Plus**

| 对比项 | MyBatis-Plus | JPA/Hibernate |
|--------|--------------|---------------|
| SQL 可控性 | 高（可写 XML） | 低（自动生成） |
| 学习曲线 | 低 | 中高 |
| 国内生态 | 极好 | 一般 |
| 复杂查询 | 灵活 | 繁琐 |
| 缓存支持 | 需自行集成 | 一级/二级缓存内置 |

**决策理由：**
- 团队更熟悉 MyBatis 系
- 需要精细控制 SQL 的场景（如 VTuber 搜索的模糊匹配）
- 国内文档和社区支持更好

---

## 三、依赖版本锁定

### 前端（package.json 关键依赖）

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "framer-motion": "^11.0.0",
    "zustand": "^4.5.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "vite": "^5.0.0",
    "tailwindcss": "^3.4.0",
    "typescript": "^5.3.0"
  }
}
```

### 后端（pom.xml 关键依赖）

```xml
<properties>
    <java.version>21</java.version>
    <spring-boot.version>3.2.0</spring-boot.version>
    <mybatis-plus.version>3.5.5</mybatis-plus.version>
</properties>
```

---

## 四、未来技术演进预留

| 场景 | 预留方案 |
|------|----------|
| 对战模式实时同步 | WebSocket（Spring 原生支持） |
| 排行榜缓存 | Redis（后期引入，前期用数据库） |
| 图片存储 | 本地 → OSS（后期迁移） |
| 前端 SSR/SEO | Next.js（如需迁移，组件可复用） |

---

*文档版本：v1.0 | 更新日期：2026-08-18*
