# VTuber 相关表设计

> 本文档详细定义 VTuber 主表和团体表的结构。
> 返回 [AGENTS.md](../../AGENTS.md) | [数据库总览](./001-schema-overview.md)

---

## 一、VTuber 主表（vtuber）

### 1.1 表结构

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 自增主键 |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | vtbs.moe 原始 UUID |
| name_cn | VARCHAR(100) | | 中文名 |
| name_en | VARCHAR(100) | | 英文名 |
| name_jp | VARCHAR(100) | | 日文名 |
| name_default | VARCHAR(20) | | 默认语言：cn/en/jp |
| aliases | JSON | | 别名数组，用于搜索 |
| debut_year | INT | | 出道年份 |
| debut_date | DATE | | 出道日期（精确到日） |
| region | VARCHAR(50) | INDEX | 所属地区 |
| group_id | BIGINT | FK, INDEX | 所属团体 ID |
| group_name | VARCHAR(100) | | 所属团体名称（冗余） |
| activity_status | VARCHAR(20) | INDEX | 活动状态 |
| gender | VARCHAR(20) | | 性别 |
| hair_color | JSON | | 发色数组 |
| eye_color | JSON | | 瞳色数组 |
| outfit_theme | VARCHAR(200) | | 服装主题描述 |
| fan_name | VARCHAR(100) | | 粉丝群体称呼 |
| symbol | VARCHAR(50) | | 标志性 emoji/符号 |
| representative_color | VARCHAR(7) | | 代表色 HEX |
| platforms | JSON | | 主要活动平台数组 |
| languages | JSON | | 使用语言数组 |
| avatar_url | VARCHAR(500) | | 头像 URL |
| data_status | VARCHAR(20) | INDEX | 数据状态 |
| data_source | VARCHAR(20) | | 数据来源 |
| locked_fields | JSON | | 被手动锁定的字段名数组 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 1.2 字段详细说明

#### 名称字段组

| 字段 | 来源 | 优先级 | 备注 |
|------|------|--------|------|
| name_cn | vtbs.moe / 爬虫 / 手动 | manual > crawler | 中文圈常用名 |
| name_en | vtbs.moe / 爬虫 / 手动 | manual > crawler | 国际通用名 |
| name_jp | vtbs.moe / 爬虫 / 手动 | manual > crawler | 日文原名 |
| name_default | 系统计算 | - | 根据数据完整度自动选择 |
| aliases | 爬虫 / 手动 | manual > crawler | 包含昵称、曾用名、梗名 |

#### 状态字段组

| 字段 | 可选值 | 说明 |
|------|--------|------|
| activity_status | active / graduated / hiatus / suspended | 活动状态 |
| data_status | raw / candidate / active / verified | 数据生命周期 |
| data_source | crawler / manual / admin | 数据来源 |

#### 外貌特征字段组

| 字段 | 格式 | 示例 |
|------|------|------|
| hair_color | JSON 数组 | `["蓝", "白"]` |
| eye_color | JSON 数组 | `["蓝"]` |
| representative_color | HEX 字符串 | `#1E90FF` |
| outfit_theme | 文本描述 | `海洋风、水手服` |

### 1.3 数据状态机

```
┌─────────┐     运营挑选      ┌──────────┐     爬虫补全      ┌────────┐
│   raw   │ ───────────────→ │ candidate│ ───────────────→ │ active │
│ (全量池) │                  │ (候选队列)│                  │(正式池) │
└─────────┘                  └──────────┘                  └────────┘
                                                              │
                                                              ↓ 人工核实
                                                           ┌────────┐
                                                           │verified│
                                                           │(已核实) │
                                                           └────────┘
```

| 状态 | 说明 | 可操作 |
|------|------|--------|
| raw | 从 vtbs.moe 导入的原始数据，仅有基础信息 | 运营可挑选进候选队列 |
| candidate | 运营挑选的候选 VTuber，等待爬虫补全 | 爬虫可补全数据 |
| active | 爬虫已补全数据，进入正式竞猜池 | 可用于每日/单人模式 |
| verified | 人工核实过的数据，质量最高 | 优先级最高，爬虫不可覆盖 |

### 1.4 数据优先级与锁定机制

**优先级规则（高 → 低）：**
1. `manual`（运营手动编辑）— 最高优先级，永久锁定
2. `admin`（管理员直接修改数据库）— 高优先级
3. `crawler`（爬虫自动补全）— 低优先级，可被覆盖

**锁定机制：**
- 当运营在后台手动编辑某字段时，该字段加入 `locked_fields` 数组
- 爬虫更新时，跳过 `locked_fields` 中的字段
- 只有运营可以解锁字段（在后台取消锁定）

**示例：**
```json
{
  "id": 123,
  "name_cn": "噶呜·古拉",
  "hair_color": ["蓝", "白"],
  "data_source": "manual",
  "locked_fields": ["hair_color", "fan_name"]
}
```

---

## 二、团体表（vtuber_group）

### 2.1 表结构

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 自增主键 |
| name | VARCHAR(100) | NOT NULL | 团体名称 |
| name_en | VARCHAR(100) | | 英文名 |
| region | VARCHAR(50) | | 所属地区 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

### 2.2 初始数据

| name | name_en | region |
|------|---------|--------|
| Hololive | Hololive Production | 日本 |
| Hololive EN | Hololive English | 英语圈 |
| Nijisanji | Nijisanji | 日本 |
| Nijisanji EN | Nijisanji English | 英语圈 |
| VirtuaReal | VirtuaReal | 中国 |
| 个人势 | Independent | - |
| 其他 | Others | - |

---

## 三、索引设计

| 索引名 | 字段 | 类型 | 用途 |
|--------|------|------|------|
| PRIMARY | id | 主键 | 内部关联 |
| uk_uuid | uuid | 唯一索引 | 外部标识 |
| idx_region | region | 普通索引 | 按地区筛选 |
| idx_group_id | group_id | 普通索引 | 按团体筛选 |
| idx_data_status | data_status | 普通索引 | 按数据状态筛选 |
| idx_activity_status | activity_status | 普通索引 | 按活动状态筛选 |
| idx_debut_year | debut_year | 普通索引 | 按出道年份筛选 |

---

## 四、搜索优化

### 4.1 名称搜索

支持多语言名称和别名的模糊搜索：

```sql
-- MyBatis-Plus 示例
SELECT * FROM vtuber 
WHERE data_status IN ('active', 'verified')
AND (
    name_cn LIKE CONCAT('%', #{keyword}, '%')
    OR name_en LIKE CONCAT('%', #{keyword}, '%')
    OR name_jp LIKE CONCAT('%', #{keyword}, '%')
    OR JSON_CONTAINS(aliases, JSON_QUOTE(#{keyword}))
)
LIMIT 20;
```

### 4.2 搜索权重

| 匹配位置 | 权重 | 说明 |
|----------|------|------|
| name_default 完全匹配 | 100 | 最高优先级 |
| name_cn/en/jp 完全匹配 | 90 | 次高优先级 |
| aliases 完全匹配 | 80 | 别名匹配 |
| 前缀匹配 | 60 | 如 "Gura" 匹配 "Gawr Gura" |
| 包含匹配 | 40 | 模糊匹配 |

---

*文档版本：v1.0 | 更新日期：2026-08-18*
