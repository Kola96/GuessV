# M4：前端游戏界面 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.
> 返回 [路线图](../000-roadmap.md) | [AGENTS.md](../../../AGENTS.md)

**Goal:** 在浏览器里完整可玩每日模式和单人模式，ACG 治系深色主题 + 中等动效。

**Architecture:** React 18 + Vite + TypeScript + Tailwind CSS + Framer Motion + Zustand + Axios。开发时 Vite proxy 转发 /api 到 :8080。打包产物拷入后端 static，生产单 Jar 部署。

**Tech Stack:** React 18.2, Vite 5, Tailwind 3.4, Framer Motion 11, Zustand 4.5, Axios 1.6

## Global Constraints

- TypeScript，严格模式
- 包名：根目录 `frontend/`
- 前端 dev 运行目录：`frontend/`，proxy 到 `http://localhost:8080`
- 配色：ACG 治系深色（深蓝紫底 + 粉/星点高光）
- 动效：中等（卡片滑入 + 逐行揭示 + 胜利彩花）
- 头像：首字母圆形占位（用代表色作背景）
- 提交信息：`feat(frontend): xxx`

## 设计规范

### 配色（深色主题）

```tailwind
背景层级：
  bg-base:    #0f0a1e  (深蓝紫黑，最底层)
  bg-surface: #1a1330  (卡片/面板底)
  bg-elevated:#241a3d  (悬浮/输入框)

主色（VTuber 紫粉）：
  primary:    #c084fc  (紫，主按钮/链接)
  secondary:  #f472b6  (粉，高亮/强调)

语义色（对比结果）：
  exact:  #4ade80  (绿，完全匹配)
  partial:#fbbf24  (橙，部分匹配)
  none:   #6b7280  (灰，不匹配)

文字：
  text-primary:   #f3f4f6  (近白)
  text-secondary: #a5b4fc  (淡紫，副文字)
  text-muted:     #7c7a9c   (暗紫灰，弱化)
```

### 字体

- 标题：`'Noto Sans SC', sans-serif`（中文支持好）
- 正文：同上
- 数字/代码：`'JetBrains Mono', monospace`

### 动效清单

| 场景 | 动效 | Framer Motion 实现 |
|------|------|---------------------|
| 猜测卡片入场 | 从下滑入 + 淡入 | `initial={{y:30,opacity:0}} animate={{y:0,opacity:1}}` |
| 属性逐行揭示 | staggerChildren 0.08s | 父 `variants={stagger}` 子 `animate` |
| 匹配高亮 | 闪光脉冲 1 次 | `animate={{boxShadow:['0 0 0 #xxx','0 0 20 #xxx','0 0 0 #xxx']}}` |
| 胜利 | 彩花粒子 + 标题缩放 | 自定义 Confetti 组件 |
| 失败揭晓 | 答案卡从下滑入 | `AnimatePresence` |
| 输入框抖动（错误） | x 轴抖动 3 次 | `animate={{x:[0,-5,5,-3,3,0]}}` |

### 布局

- 桌面：居中，max-width 640px
- 移动：全宽，触屏友好
- 顶栏：Logo + GuessV + 模式切换 + 用户昵称
- 状态栏：第 X 次/共 8 次
- 猜测记录：从下往上堆叠（最新在底）
- 输入区：固定底部

---

## Task 1：项目骨架

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tailwind.config.js`
- Create: `frontend/postcss.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/App.tsx`
- Create: `frontend/src/index.css`
- Create: `frontend/src/vite-env.d.ts`

- [x] Step 1：创建 package.json + 配置文件
- [x] Step 2：创建 Tailwind 配置（含自定义配色）
- [x] Step 3：创建入口 main.tsx + App.tsx + index.css
- [x] Step 4：npm install + npm run dev 验证启动
- [x] Step 5：提交

## Task 2：类型定义与 API 层

**Files:**
- Create: `frontend/src/types/index.ts`
- Create: `frontend/src/services/api.ts`
- Create: `frontend/src/services/game.ts`
- Create: `frontend/src/services/user.ts`
- Create: `frontend/src/stores/userStore.ts`

- [x] Step 1：类型定义（Vtuber / Comparison / GameInfo / GuessResponse 等）
- [x] Step 2：Axios 实例（baseURL + 拦截器自动加 X-User-Token）
- [x] Step 3：user API（init / profile / nickname）
- [x] Step 4：game API（daily / single）
- [x] Step 5：userStore（Zustand，含 LocalStorage 持久化）
- [x] Step 6：提交

## Task 3：用户初始化流程

**Files:**
- Create: `frontend/src/components/NicknameSetup.tsx`
- Create: `frontend/src/components/Header.tsx`

- [x] Step 1：NicknameSetup 弹窗（昵称输入 + 随机按钮）
- [x] Step 2：Header（Logo + 用户显示名）
- [x] Step 3：App 路由：无 token → NicknameSetup，有 token → 游戏
- [x] Step 4：提交

## Task 4：搜索输入与自动补全

**Files:**
- Create: `frontend/src/components/SearchInput.tsx`

- [x] Step 1：输入框 + 防抖搜索 + 下拉建议
- [x] Step 2：选中 V 后触发 onGuess
- [x] Step 3：提交

## Task 5：猜测卡片与对比动画

**Files:**
- Create: `frontend/src/components/GuessCard.tsx`
- Create: `frontend/src/components/ComparisonRow.tsx`
- Create: `frontend/src/components/Avatar.tsx`（首字母占位）

- [x] Step 1：Avatar（首字母 + 代表色背景）
- [x] Step 2：ComparisonRow（单个属性的展示与高亮动画）
- [x] Step 3：GuessCard（V 名 + 8 属性横排，入场动画 + stagger）
- [x] Step 4：提交

## Task 6：每日模式页面

**Files:**
- Create: `frontend/src/features/daily/DailyGame.tsx`
- Create: `frontend/src/stores/dailyStore.ts`

- [x] Step 1：dailyStore（Zustand，游戏状态）
- [x] Step 2：DailyGame 页面（信息 + 猜测记录 + 输入）
- [x] Step 3：提交

## Task 7：单人模式页面

**Files:**
- Create: `frontend/src/features/single/SingleGame.tsx`
- Create: `frontend/src/features/single/PoolSelect.tsx`
- Create: `frontend/src/stores/singleStore.ts`

- [x] Step 1：PoolSelect（题库选择网格）
- [x] Step 2：singleStore
- [x] Step 3：SingleGame 页面（含重开）
- [x] Step 4：提交

## Task 8：胜负反馈与路由

**Files:**
- Create: `frontend/src/components/Confetti.tsx`
- Create: `frontend/src/components/ResultBanner.tsx`
- Modify: `frontend/src/App.tsx`（React Router）

- [x] Step 1：Confetti 粒子动画
- [x] Step 2：ResultBanner（胜利/失败横幅）
- [x] Step 3：App 路由（/ = 每日，/single = 单人）
- [x] Step 4：提交

## Task 9：响应式与打包联调

- [x] Step 1：移动端适配检查
- [x] Step 2：构建产物拷入后端 static
- [x] Step 3：后端 SPA fallback 验证
- [x] Step 4：端到端联调（完整一局）
- [x] Step 5：提交 + 更新路线图

---

## M4 完成标准

- [x] `npm run dev` 前端可启动，proxy 到后端
- [x] 首次访问弹昵称设置，输入后进入游戏
- [x] 每日模式可完整玩一局（猜→对比→胜负）
- [x] 单人模式可选题库、无限重开
- [x] 胜利彩花 + 失败揭晓动画
- [x] 移动端可用
- [x] 打包进后端 static，单 Jar 启动可访问前端

---

*M4 完成后，进入 M5（运营后台基础版）。*
