# 单人模式设计

> 本文档详细定义单人模式的游戏逻辑和实现细节。
> 返回 [AGENTS.md](../../AGENTS.md)

---

## 一、核心规则

| 规则 | 说明 |
|------|------|
| 目标选定 | 根据用户选择的题库标签随机选取 |
| 题库选择 | 日V/国V/热门/全量/自定义标签 |
| 尝试次数 | 默认 8 次（可配置） |
| 重开机制 | 无限重开，每次重新随机 |
| 状态存储 | 服务端会话 + 可选 LocalStorage 备份 |

---

## 二、题库系统

### 2.1 题库标签

| 标签 | 筛选规则 | 预计数量 |
|------|----------|----------|
| 全量 | data_status: active/verified | ~150 |
| 日V | region: 日本 | ~80 |
| 国V | region: 中国 | ~40 |
| 英语圈 | region: 英语圈 | ~30 |
| 热门 | data_status: verified | ~20 |
| Hololive | group_name: Hololive% | ~35 |
| Nijisanji | group_name: Nijisanji% | ~40 |

### 2.2 自定义标签（运营配置）

运营可在后台创建自定义标签，通过 `filter_rule` JSON 定义筛选条件：

```json
{
  "region": "日本",
  "group_name": "Hololive%",
  "activity_status": "active",
  "min_debut_year": 2020
}
```

---

## 三、游戏会话管理

### 3.1 会话创建

```
用户选择题库标签
    ↓
POST /api/game/single/start { poolTag: "全量" }
    ↓
服务端：
  1. 根据 filter_rule 查询候选 VTuber 列表
  2. 随机选取一个作为目标
  3. 创建 game_record（mode: single, pool_tag: "全量"）
  4. 生成 sessionId（UUID）
  5. 缓存 sessionId → targetId 映射（Redis/内存）
    ↓
返回 sessionId 和基本信息
```

### 3.2 会话存储

| 存储 | 内容 | 过期时间 |
|------|------|----------|
| 服务端缓存 | sessionId → targetId, attempts, guesses | 24 小时 |
| 数据库 | game_record 完整记录 | 永久 |

### 3.3 会话恢复

- 单人模式会话默认 24 小时内有效
- 刷新页面后可通过 sessionId 恢复
- 超过 24 小时或主动结束后，sessionId 失效

---

## 四、无限重开机制

### 4.1 重开流程

```
用户点击 "重新开始"
    ↓
POST /api/game/single/start { poolTag: "same" }
    ↓
服务端创建新会话，返回新 sessionId
    ↓
前端重置游戏界面
```

### 4.2 重开策略

| 策略 | 说明 |
|------|------|
| 同一标签 | 从同一题库重新随机 |
| 更换标签 | 可切换不同题库 |
| 无冷却 | 不限制重开频率 |

---

## 五、与每日模式的区别

| 维度 | 每日模式 | 单人模式 |
|------|----------|----------|
| 目标来源 | 服务端统一 | 用户选择题库 |
| 会话标识 | user_id + date | sessionId |
| 重开限制 | 每日一局 | 无限重开 |
| 全球统一 | 是 | 否 |
| 数据统计 | 计入日榜 | 计入总统计 |

---

## 六、配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| single.maxAttempts | 8 | 单人模式最大尝试次数 |
| single.sessionExpireHours | 24 | 会话过期时间（小时） |
| single.allowCustomPool | true | 是否允许自定义题库 |

---

*文档版本：v1.0 | 更新日期：2026-08-18*
