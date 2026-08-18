# 游戏相关表设计

> 本文档详细定义每日目标、游戏记录、题库标签、操作日志等表的结构。
> 返回 [AGENTS.md](../../AGENTS.md) | [数据库总览](./001-schema-overview.md)

---

## 一、每日目标表（daily_target）

### 1.1 表结构

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 自增主键 |
| target_date | DATE | UNIQUE, NOT NULL | 目标日期（UTC+8） |
| vtuber_id | BIGINT | FK, NOT NULL | 目标 VTuber ID |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

### 1.2 设计说明

- **每日一条**：每天 00:00（UTC+8）插入新记录
- **排除重复**：近 30 天内已选过的 VTuber 不再入选
- **全球统一**：所有玩家查询同一天的记录

### 1.3 查询示例

```java
// 获取今日目标
DailyTarget today = dailyTargetMapper.selectOne(
    new QueryWrapper<DailyTarget>()
        .eq("target_date", LocalDate.now())
);
```

---

## 二、游戏记录表（game_record）

### 2.1 表结构

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 自增主键 |
| user_id | BIGINT | FK, INDEX, NOT NULL | 用户 ID |
| mode | VARCHAR(20) | INDEX, NOT NULL | 游戏模式 |
| target_id | BIGINT | FK, NOT NULL | 目标 VTuber ID |
| pool_tag | VARCHAR(50) | | 题库标签（single 模式） |
| attempts | INT | NOT NULL | 尝试次数 |
| max_attempts | INT | NOT NULL | 最大尝试次数 |
| is_win | BOOLEAN | NOT NULL | 是否胜利 |
| guesses | JSON | | 猜测历史数组 |
| started_at | DATETIME | NOT NULL | 开始时间 |
| finished_at | DATETIME | | 结束时间 |

### 2.2 游戏模式枚举

| 值 | 说明 |
|----|------|
| daily | 每日模式 |
| single | 单人模式 |
| multi | 对战模式（预留） |

### 2.3 guesses JSON 结构

```json
[
  {
    "vtuberId": 123,
    "vtuberName": "Gawr Gura",
    "attemptNumber": 1,
    "isCorrect": false,
    "comparison": {
      "region": { "value": "英语圈", "match": "exact" },
      "group": { "value": "Hololive EN", "match": "exact" },
      "debutYear": { "value": 2020, "match": "higher", "direction": "↓" }
    },
    "guessedAt": "2026-08-18T10:30:00Z"
  }
]
```

### 2.4 索引设计

| 索引名 | 字段 | 类型 | 用途 |
|--------|------|------|------|
| PRIMARY | id | 主键 | 内部关联 |
| idx_user_id | user_id | 普通索引 | 查询用户历史 |
| idx_mode | mode | 普通索引 | 按模式统计 |
| idx_user_mode_date | (user_id, mode, started_at) | 联合索引 | 查询用户当日记录 |

---

## 三、题库标签表（pool_tag）

### 3.1 表结构

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 自增主键 |
| tag_name | VARCHAR(50) | UNIQUE, NOT NULL | 标签名 |
| description | VARCHAR(200) | | 标签描述 |
| filter_rule | JSON | NOT NULL | 筛选规则 |
| is_active | BOOLEAN | DEFAULT TRUE | 是否启用 |
| sort_order | INT | DEFAULT 0 | 排序权重 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

### 3.2 初始标签

| tag_name | description | filter_rule |
|----------|-------------|-------------|
| 全量 | 所有可用 VTuber | `{"data_status": ["active", "verified"]}` |
| 日V | 日本地区 VTuber | `{"region": "日本", "data_status": ["active", "verified"]}` |
| 国V | 中国地区 VTuber | `{"region": "中国", "data_status": ["active", "verified"]}` |
| 英语圈 | 英语地区 VTuber | `{"region": "英语圈", "data_status": ["active", "verified"]}` |
| 热门 | 高知名度 VTuber | `{"data_status": ["verified"], "min_subscriber": 100000}` |
| Hololive | Hololive 所属 | `{"group_name": "Hololive%", "data_status": ["active", "verified"]}` |
| Nijisanji | Nijisanji 所属 | `{"group_name": "Nijisanji%", "data_status": ["active", "verified"]}` |

### 3.3 filter_rule JSON 结构

```json
{
  "region": "日本",
  "group_name": "Hololive%",
  "activity_status": "active",
  "data_status": ["active", "verified"],
  "min_debut_year": 2018,
  "max_debut_year": 2024,
  "min_subscriber": 100000
}
```

---

## 四、操作日志表（operation_log）

### 4.1 表结构

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 自增主键 |
| operator_id | BIGINT | FK, INDEX | 操作人 ID |
| operation_type | VARCHAR(50) | INDEX, NOT NULL | 操作类型 |
| target_type | VARCHAR(50) | NOT NULL | 目标类型 |
| target_id | BIGINT | NOT NULL | 目标 ID |
| field_name | VARCHAR(50) | | 被修改的字段名 |
| old_value | TEXT | | 旧值 |
| new_value | TEXT | | 新值 |
| ip_address | VARCHAR(50) | | 操作 IP |
| user_agent | VARCHAR(500) | | 浏览器信息 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 操作时间 |

### 4.2 操作类型枚举

| 值 | 说明 |
|----|------|
| edit_vtuber | 编辑 VTuber 属性 |
| promote_vtuber | 提升 VTuber 状态 |
| verify_vtuber | 核实 VTuber 数据 |
| unlock_field | 解锁字段 |
| lock_field | 锁定字段 |
| create_pool_tag | 创建题库标签 |
| edit_pool_tag | 编辑题库标签 |
| trigger_crawler | 手动触发爬虫 |

### 4.3 目标类型枚举

| 值 | 说明 |
|----|------|
| vtuber | VTuber 数据 |
| group | 团体数据 |
| pool_tag | 题库标签 |
| daily_target | 每日目标 |

---

## 五、数据关系图

```
┌─────────────────┐         ┌─────────────────┐
│   daily_target  │         │   game_record   │
│                 │         │                 │
│ target_date ────┼────┐    │ user_id ────────┼───┐
│ vtuber_id ──────┼────┼────┤ target_id       │   │
└─────────────────┘    │    │ mode            │   │
                       │    │ pool_tag ───────┼───┼───┐
                       │    └─────────────────┘   │   │
                       │                          │   │
                       ↓                          ↓   │
                ┌─────────────────┐         ┌─────────┐
                │     vtuber      │         │  user   │
                │                 │         │         │
                │ id ◄────────────┘         │ id ◄────┘
                │ name_cn                   │ uuid
                │ ...                       │ nickname
                └─────────────────┘         └─────────┘
                       ↑
                       │
                ┌─────────────────┐
                │   pool_tag      │
                │                 │
                │ tag_name ◄──────┘
                │ filter_rule
                └─────────────────┘
```

---

*文档版本：v1.0 | 更新日期：2026-08-18*
