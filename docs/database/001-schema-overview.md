# 数据库设计总览

> 本文档概述 GuessV 数据库的整体设计原则和表关系。
> 返回 [AGENTS.md](../../AGENTS.md)

---

## 一、数据库切换策略

| 环境 | 数据库 | 配置文件 | 说明 |
|------|--------|----------|------|
| 开发/测试 | SQLite 3 | `application-dev.yml` | 零配置，单文件存储于 `data/guessv.db` |
| 生产 | MySQL 8 | `application-prod.yml` | 通过环境变量注入连接信息 |

### 切换原则

1. **禁止数据库方言**：所有 SQL 使用 MyBatis-Plus 标准方法
2. **标准字段类型**：使用 `LocalDateTime` 而非 `Date`，`BIGINT` 而非 `LONG`
3. **分页标准化**：使用 MyBatis-Plus 分页插件，禁止 `LIMIT` 关键字
4. **配置即切换**：生产环境仅需修改配置文件，无需修改代码

---

## 二、表关系图

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   vtuber_group  │     │     vtuber      │     │   daily_target  │
│   (团体表)       │◄────│   (VTuber主表)   │────►│   (每日目标表)   │
│                 │ 1:N │                 │ N:1 │                 │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                                 │
                                 │ 1:N
                                 ↓
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   operation_log │     │   game_record   │     │    pool_tag     │
│   (操作日志表)   │◄────│   (游戏记录表)   │────►│   (题库标签表)   │
│                 │ N:1 │                 │ N:1 │                 │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                                 │
                                 │ N:1
                                 ↓
                          ┌─────────────────┐
                          │      user       │
                          │    (用户表)      │
                          │                 │
                          └────────┬────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │ 1:N                │ 1:N                │ 1:N
              ↓                    ↓                    ↓
       ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
       │ room_player │      │nickname_hist│      │  user_stat  │
       │(房间玩家表)  │      │ (昵称历史表) │      │ (用户统计表) │
       │  (预留)      │      │   (预留)     │      │   (预留)     │
       └──────┬──────┘      └─────────────┘      └─────────────┘
              │
              │ N:1
              ↓
       ┌─────────────┐
       │    room     │
       │   (房间表)   │
       │   (预留)     │
       └─────────────┘
```

---

## 三、核心表清单

| 表名 | 说明 | 文档链接 |
|------|------|----------|
| `vtuber` | VTuber 主表 | [002-vtuber-table.md](./002-vtuber-table.md) |
| `vtuber_group` | 团体表 | [002-vtuber-table.md](./002-vtuber-table.md) |
| `user` | 用户表 | [003-user-table.md](./003-user-table.md) |
| `daily_target` | 每日目标表 | [004-game-tables.md](./004-game-tables.md) |
| `game_record` | 游戏记录表 | [004-game-tables.md](./004-game-tables.md) |
| `pool_tag` | 题库标签表 | [004-game-tables.md](./004-game-tables.md) |
| `room` | 房间表（对战预留） | [005-multi-mode-tables.md](./005-multi-mode-tables.md) |
| `room_player` | 房间玩家表（对战预留） | [005-multi-mode-tables.md](./005-multi-mode-tables.md) |
| `operation_log` | 操作日志表 | [004-game-tables.md](./004-game-tables.md) |

---

## 四、通用字段规范

### 4.1 主键策略

| 类型 | 说明 | 示例 |
|------|------|------|
| `id` | 自增主键，内部使用 | `1`, `2`, `3` |
| `uuid` | 业务唯一标识，外部暴露 | `550e8400-e29b-41d4-a716-446655440000` |

**原则**：对外接口一律使用 `uuid`，禁止暴露自增 `id`

### 4.2 时间字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `created_at` | DATETIME | 创建时间，默认 CURRENT_TIMESTAMP |
| `updated_at` | DATETIME | 更新时间，ON UPDATE CURRENT_TIMESTAMP |

### 4.3 JSON 字段

以下字段使用 JSON 类型存储数组/对象：

| 表 | 字段 | 内容 |
|----|------|------|
| `vtuber` | `aliases` | 别名数组 `["Gura", "鲨鲨"]` |
| `vtuber` | `hair_color` | 发色数组 `["蓝", "白"]` |
| `vtuber` | `eye_color` | 瞳色数组 `["蓝"]` |
| `vtuber` | `platforms` | 平台数组 `["YouTube", "Bilibili"]` |
| `vtuber` | `languages` | 语言数组 `["日语", "英语"]` |
| `vtuber` | `locked_fields` | 锁定字段名数组 `["hair_color", "fan_name"]` |
| `game_record` | `guesses` | 猜测历史数组 |
| `pool_tag` | `filter_rule` | 筛选规则对象 |

---

## 五、索引设计原则

| 场景 | 索引类型 | 示例 |
|------|----------|------|
| 主键查询 | PRIMARY KEY | `id` |
| 业务唯一 | UNIQUE INDEX | `uuid`, `room_code` |
| 外键关联 | INDEX | `user_id`, `vtuber_id` |
| 状态筛选 | INDEX | `data_status`, `activity_status` |
| 时间范围 | INDEX | `target_date`, `created_at` |
| 联合查询 | COMPOSITE INDEX | `(nickname, game_id)` |

---

## 六、数据迁移策略

### 6.1 SQLite → MySQL 迁移步骤

1. **导出 SQLite 数据**：
   ```bash
   sqlite3 data/guessv.db .dump > backup.sql
   ```

2. **转换 SQL 方言**：
   - `AUTOINCREMENT` → `AUTO_INCREMENT`
   - `DATETIME` 默认值语法调整
   - JSON 类型确认（MySQL 5.7+ 支持）

3. **导入 MySQL**：
   ```bash
   mysql -u root -p guessv < backup_converted.sql
   ```

4. **验证数据一致性**：
   - 记录数对比
   - 关键字段抽样检查

### 6.2 版本控制

使用 Flyway 或 Liquibase 管理数据库变更：

```
src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__add_user_table.sql
├── V3__add_multi_mode_tables.sql
└── ...
```

---

*文档版本：v1.0 | 更新日期：2026-08-18*
