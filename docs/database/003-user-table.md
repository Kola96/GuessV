# 用户表设计

> 本文档详细定义用户表及相关表的结构。
> 返回 [AGENTS.md](../../AGENTS.md) | [数据库总览](./001-schema-overview.md)

---

## 一、用户表（user）

### 1.1 表结构

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 自增主键 |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | 用户唯一标识（内部使用） |
| nickname | VARCHAR(16) | NOT NULL | 昵称（可重复） |
| game_id | VARCHAR(4) | NOT NULL | 游戏ID（如 AB12） |
| username | VARCHAR(20) | UNIQUE | 登录用户名（绑定后） |
| password_hash | VARCHAR(100) | | 密码哈希（BCrypt） |
| email | VARCHAR(100) | | 邮箱（绑定后） |
| oauth_provider | VARCHAR(20) | | 第三方登录：google/discord/twitter |
| oauth_id | VARCHAR(100) | | 第三方平台 ID |
| avatar_url | VARCHAR(500) | | 头像 URL |
| device_fingerprint | VARCHAR(64) | | 设备指纹（防刷） |
| is_anonymous | BOOLEAN | DEFAULT TRUE | 是否匿名用户 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| last_active_at | DATETIME | | 最后活跃时间 |

### 1.2 联合唯一索引

```sql
-- 昵称 + 游戏ID 联合唯一，用于显示标识
UNIQUE KEY uk_nickname_gameid (nickname, game_id)
```

**设计理由：**
- 允许大量用户叫 "小明"
- 通过 `小明#AB12` 区分不同用户
- 实际内部关联使用 `uuid`

### 1.3 字段详细说明

#### 标识字段组

| 字段 | 生成时机 | 可变性 | 用途 |
|------|----------|--------|------|
| uuid | 创建时 | 不可变 | 内部主键，API 标识 |
| nickname | 创建时/修改时 | 可变 | 显示名称 |
| game_id | 创建时 | 不可变 | 区分重名用户 |
| username | 绑定时 | 可变（预留） | 登录凭证 |

#### 认证字段组

| 字段 | 匿名用户 | 绑定用户 |
|------|----------|----------|
| username | NULL | 必填，唯一 |
| password_hash | NULL | 必填，BCrypt |
| email | NULL | 可选 |
| oauth_provider | NULL | 可选 |
| oauth_id | NULL | 可选 |

#### 安全字段组

| 字段 | 说明 |
|------|------|
| device_fingerprint | 浏览器指纹，用于防刷和找回 |
| is_anonymous | 标记是否已绑定账号 |

---

## 二、昵称历史表（nickname_history）— 预留

### 2.1 表结构

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 自增主键 |
| user_id | BIGINT | FK, NOT NULL | 用户 ID |
| old_nickname | VARCHAR(16) | | 旧昵称 |
| new_nickname | VARCHAR(16) | NOT NULL | 新昵称 |
| changed_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 修改时间 |

### 2.2 用途

- 追踪用户改名历史
- 防止恶意改名骚扰
- 支持 "曾用名" 展示（远期）

---

## 三、用户统计表（user_stat）— 预留

### 3.1 表结构

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 自增主键 |
| user_id | BIGINT | FK, UNIQUE, NOT NULL | 用户 ID |
| total_games | INT | DEFAULT 0 | 总游戏场次 |
| total_wins | INT | DEFAULT 0 | 总胜利场次 |
| current_streak | INT | DEFAULT 0 | 当前连胜 |
| max_streak | INT | DEFAULT 0 | 最高连胜 |
| total_attempts | INT | DEFAULT 0 | 总尝试次数 |
| avg_attempts | DECIMAL(3,1) | | 平均尝试次数 |
| last_played_at | DATETIME | | 最后游戏时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 3.2 用途

- 排行榜数据源
- 个人成就系统
- 游戏数据分析

---

## 四、游戏ID 生成规则

### 4.1 格式

- 长度：4 位
- 字符集：大写字母（A-Z）+ 数字（0-9）
- 排除易混淆字符：I, O, 0, 1

### 4.2 生成算法

```java
public String generateGameId() {
    String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    StringBuilder sb = new StringBuilder(4);
    Random random = new Random();
    for (int i = 0; i < 4; i++) {
        sb.append(chars.charAt(random.nextInt(chars.length())));
    }
    return sb.toString();
}
```

### 4.3 冲突处理

- 生成后检查是否已存在
- 冲突时重新生成，最多重试 10 次
- 10 次后仍冲突，扩展为 5 位

---

## 五、索引设计

| 索引名 | 字段 | 类型 | 用途 |
|--------|------|------|------|
| PRIMARY | id | 主键 | 内部关联 |
| uk_uuid | uuid | 唯一索引 | API 标识 |
| uk_username | username | 唯一索引 | 登录查询 |
| uk_nickname_gameid | (nickname, game_id) | 联合唯一 | 显示标识 |
| idx_device_fingerprint | device_fingerprint | 普通索引 | 防刷查询 |
| idx_created_at | created_at | 普通索引 | 注册时间统计 |

---

## 六、数据示例

### 6.1 匿名用户

```json
{
  "id": 1001,
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "nickname": "小明",
  "game_id": "AB12",
  "username": null,
  "password_hash": null,
  "is_anonymous": true,
  "device_fingerprint": "fp_abc123...",
  "created_at": "2026-08-18 10:00:00"
}
```

### 6.2 绑定用户

```json
{
  "id": 1002,
  "uuid": "660e8400-e29b-41d4-a716-446655440001",
  "nickname": "GuraFan",
  "game_id": "XY9Z",
  "username": "gura_lover",
  "password_hash": "$2a$10$...",
  "email": "gura@example.com",
  "is_anonymous": false,
  "created_at": "2026-08-18 11:00:00"
}
```

---

*文档版本：v1.0 | 更新日期：2026-08-18*
