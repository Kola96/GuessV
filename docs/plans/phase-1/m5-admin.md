# M5：运营后台基础版 - 实施计划

> 返回 [路线图](../000-roadmap.md) | [AGENTS.md](../../../AGENTS.md)

**Goal:** 管理员可登录后台，完成 VTuber 状态流转和属性编辑，数据看板可见概况。

**Architecture:** 复用 frontend 项目，新增 `/admin` 路由分支。后端新增 admin API（独立 JWT + BCrypt）。前端后台页面用独立布局（不共享游戏 UI）。

## Global Constraints

- 后台前端路由：`/admin/*`（与游戏页面 `/` 共存）
- 管理员凭证：`Authorization: Bearer {token}`（与用户 `X-User-Token` 区分）
- 管理员账号：启动时自动初始化（用户名 admin，密码从环境变量读，默认 admin123）
- 密码加密：BCrypt
- 后台页面风格：深色主题保持一致，但布局更紧凑（表格密集型）

---

## Task 1：后端 - 管理员认证

**Files:**
- Modify: `pom.xml`（加 spring-security-crypto 用于 BCrypt）
- Create: `AdminAuthInterceptor.java`
- Create: `AdminService.java`
- Create: `AdminController.java`
- Create: `AdminMapper` 或复用 UserMapper
- Modify: `schema.sql`（加 admin 表）
- Modify: `WebMvcConfig`（admin 拦截器 + 白名单调整）

- [ ] Step 1：schema.sql 加 admin 表
- [ ] Step 2：AdminService（登录 + BCrypt + JWT）
- [ ] Step 3：AdminAuthInterceptor
- [ ] Step 4：AdminController（POST /api/admin/login）
- [ ] Step 5：启动时自动初始化 admin 账号
- [ ] Step 6：测试 + 提交

## Task 2：后端 - VTuber 管理 API

**Files:**
- Create: `AdminVtuberController.java`
- Create: `AdminVtuberService.java`
- Create: `VtuberDetailVO.java`（含全部字段）

- [ ] Step 1：列表 API（分页 + 状态筛选 + 搜索）
- [ ] Step 2：详情 API
- [ ] Step 3：编辑 API（自动锁定字段）
- [ ] Step 4：状态流转 API（promote/verify）
- [ ] Step 5：解锁字段 API
- [ ] Step 6：数据看板 API
- [ ] Step 7：测试 + 提交

## Task 3：前端 - 后台骨架

**Files:**
- Create: `frontend/src/features/admin/AdminApp.tsx`
- Create: `frontend/src/features/admin/AdminLayout.tsx`
- Create: `frontend/src/features/admin/AdminLogin.tsx`
- Create: `frontend/src/stores/adminStore.ts`
- Modify: `App.tsx`（加 /admin 路由分支）

- [ ] Step 1：adminStore（token + LocalStorage）
- [ ] Step 2：AdminLogin 页面
- [ ] Step 3：AdminLayout（侧边栏 + 内容区）
- [ ] Step 4：App.tsx 路由分流（/admin/* → 后台，其余 → 游戏）
- [ ] Step 5：提交

## Task 4：前端 - VTuber 管理页面

**Files:**
- Create: `AdminVtuberList.tsx`
- Create: `AdminVtuberEdit.tsx`
- Create: `AdminDashboard.tsx`

- [ ] Step 1：Dashboard（统计卡片）
- [ ] Step 2：VTuber 列表（表格 + 筛选 + 搜索 + 分页）
- [ ] Step 3：VTuber 编辑页（表单 + 字段锁定）
- [ ] Step 4：状态流转操作按钮
- [ ] Step 5：提交

## Task 5：收尾验证

- [ ] Step 1：全量测试
- [ ] Step 2：手动验证（登录 → 列表 → 编辑 → 状态流转）
- [ ] Step 3：更新路线图
- [ ] Step 4：提交 + 推送

---

## M5 完成标准

- [ ] 管理员可登录后台
- [ ] 可浏览 VTuber 列表（按状态筛选/搜索/分页）
- [ ] 可编辑 VTuber 属性（自动锁定）
- [ ] 可执行状态流转（raw→candidate→active→verified）
- [ ] 数据看板显示各状态数量
- [ ] 全部测试通过

---

*M5 完成后，Phase 1 MVP 全部完成。*
