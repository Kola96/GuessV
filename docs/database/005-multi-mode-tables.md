# 对战模式表设计（预留）

> 本文档定义对战模式相关的表结构，第一期不实现，但数据库设计必须预留。
> 返回 [AGENTS.md](../../AGENTS.md) | [数据库总览](./001-schema-overview.md)

---

## 一、设计原则

| 原则 | 说明 |
|------|------|
| 架构预留 | 表结构提前设计，避免后期大规模迁移 |
| 接口兼容 | 游戏逻辑层预留对战模式接口 |
| 渐进实现 | 第一期只建表，不实现业务逻辑 |

---

## 二、房间表（room）

### 2.1 表结构

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 自增主键 |
| room_code | VARCHAR(10) | UNIQUE, NOT NULL | 房间码（6位随机字符） |
| status | VARCHAR(20) | INDEX, NOT NULL | 房间状态 |
| game_mode | VARCHAR(20) | NOT NULL | 对战规则 |
| target_id | BIGINT | FK, NOT NULL | 目标 VTuber ID |
| max_players | INT | NOT NULL | 最大玩家数 |
| current_players | INT | DEFAULT 0 | 当前玩家数 |
| winner_id | BIGINT | FK | 获胜者 ID |
| config | JSON | | 房间配置（尝试次数、时间限制等） |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| started_at | DATETIME | | 开始时间 |
| finished_at | DATETIME | | 结束时间 |

### 2.2 房间状态枚举

| 值 | 说明 |
|----|------|
| waiting | 等待玩家加入 |
| ready | 所有玩家已准备，可开始 |
| playing | 游戏进行中 |
| finished | 游戏已结束 |
| cancelled | 房间已取消 |

### 2.3 对战规则枚举

| 值 | 说明 |
|----|------|
| race | 竞速模式：先猜中者胜 |
| turn_based | 回合模式：轮流猜测，积分高者胜 |

### 2.4 房间配置 JSON 结构

```json
{
  "maxAttempts": 8,
  "timeLimit": 300,
  "poolTag": "全量",
  "allowSpectator": true
}
```

---

## 三、房间玩家表（room_player）

### 3.1 表结构

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 自增主键 |
| room_id | BIGINT | FK, INDEX, NOT NULL | 房间 ID |
| user_id | BIGINT | FK, INDEX, NOT NULL | 用户 ID |
| player_name | VARCHAR(50) | NOT NULL | 玩家昵称（冗余） |
| is_ready | BOOLEAN | DEFAULT FALSE | 是否已准备 |
| score | INT | DEFAULT 0 | 分数 |
| finish_rank | INT | | 最终排名 |
| attempts_used | INT | DEFAULT 0 | 已用尝试次数 |
| is_winner | BOOLEAN | DEFAULT FALSE | 是否获胜 |
| joined_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 加入时间 |
| left_at | DATETIME | | 离开时间 |

### 3.2 联合唯一索引

```sql
-- 同一房间同一用户只能有一条记录
UNIQUE KEY uk_room_user (room_id, user_id)
```

---

## 四、房间事件表（room_event）— 预留

### 4.1 表结构

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 自增主键 |
| room_id | BIGINT | FK, INDEX, NOT NULL | 房间 ID |
| event_type | VARCHAR(50) | NOT NULL | 事件类型 |
| player_id | BIGINT | FK | 触发玩家 ID |
| payload | JSON | | 事件数据 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 发生时间 |

### 4.2 事件类型枚举

| 值 | 说明 |
|----|------|
| player_join | 玩家加入 |
| player_leave | 玩家离开 |
| player_ready | 玩家准备 |
| game_start | 游戏开始 |
| guess_submit | 提交猜测 |
| game_end | 游戏结束 |
| player_win | 玩家获胜 |

---

## 五、对战模式接口预留

### 5.1 游戏会话接口扩展

```java
public interface MultiGameSession extends GameSession {
    /**
     * 加入房间
     */
    void joinRoom(String roomCode, Player player);
    
    /**
     * 离开房间
     */
    void leaveRoom();
    
    /**
     * 玩家准备
     */
    void ready();
    
    /**
     * 获取房间内所有玩家状态
     */
    List<PlayerState> getAllPlayerStates();
    
    /**
     * 广播事件（WebSocket）
     */
    void broadcast(GameEvent event);
    
    /**
     * 是否房主
     */
    boolean isHost();
    
    /**
     * 开始游戏（房主操作）
     */
    void startGame();
}
```

### 5.2 WebSocket 消息格式

```json
{
  "type": "GUESS_RESULT|PLAYER_JOIN|PLAYER_LEAVE|GAME_START|GAME_END|PLAYER_READY",
  "roomId": "xxx",
  "roomCode": "ABC123",
  "playerId": "xxx",
  "timestamp": "2026-08-18T10:00:00Z",
  "payload": {}
}
```

---

## 六、索引设计

| 表 | 索引名 | 字段 | 类型 |
|----|--------|------|------|
| room | PRIMARY | id | 主键 |
| room | uk_room_code | room_code | 唯一索引 |
| room | idx_status | status | 普通索引 |
| room_player | PRIMARY | id | 主键 |
| room_player | uk_room_user | (room_id, user_id) | 联合唯一 |
| room_player | idx_user_id | user_id | 普通索引 |
| room_event | PRIMARY | id | 主键 |
| room_event | idx_room_id | room_id | 普通索引 |

---

## 七、第一期实现范围

| 内容 | 第一期 | 说明 |
|------|--------|------|
| 建表 | ✅ | 执行 DDL 创建表结构 |
| 实体类 | ✅ | 创建 Entity 和 Mapper |
| 接口定义 | ✅ | 定义 MultiGameSession 接口 |
| 业务实现 | ❌ | 不实现具体逻辑 |
| WebSocket | ❌ | 不配置 WebSocket |
| 前端界面 | ❌ | 不开发对战 UI |

---

*文档版本：v1.0 | 更新日期：2026-08-18*
