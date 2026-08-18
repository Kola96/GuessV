# 游戏 API 设计

> 本文档定义游戏相关的 REST API 接口规范。
> 返回 [AGENTS.md](../../AGENTS.md)

---

## 一、通用规范

### 1.1 基础路径

```
/api/game
```

### 1.2 请求头

| 头部 | 必填 | 说明 |
|------|------|------|
| Content-Type | 是 | application/json |
| X-User-Token | 是 | 用户凭证（匿名或绑定用户） |

### 1.3 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 1.4 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权/Token 无效 |
| 404 | 资源不存在 |
| 409 | 冲突（如重复猜测） |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

---

## 二、每日模式 API

### 2.1 获取每日游戏信息

```
GET /api/game/daily
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "date": "2026-08-18",
    "maxAttempts": 8,
    "totalVtuberCount": 152,
    "hasPlayed": true,
    "hasWon": false,
    "attemptsUsed": 3,
    "guesses": [
      {
        "vtuberId": 123,
        "vtuberName": "Gawr Gura",
        "attemptNumber": 1,
        "isCorrect": false
      }
    ]
  }
}
```

**字段说明：**

| 字段 | 说明 |
|------|------|
| date | 当前日期（UTC+8） |
| maxAttempts | 最大尝试次数 |
| totalVtuberCount | 当前可用 VTuber 总数 |
| hasPlayed | 今日是否已玩过 |
| hasWon | 今日是否已猜中 |
| attemptsUsed | 已用尝试次数 |
| guesses | 今日猜测历史 |

> 注意：不返回目标 VTuber 的具体信息，防止作弊。

### 2.2 提交每日模式猜测

```
POST /api/game/daily/guess
```

**请求体：**

```json
{
  "vtuberId": 123
}
```

**响应（猜错）：**

```json
{
  "code": 200,
  "data": {
    "isCorrect": false,
    "isGameOver": false,
    "remainingAttempts": 5,
    "comparison": {
      "name": { "value": "Gawr Gura", "match": "exact" },
      "region": { "value": "英语圈", "match": "exact" },
      "group": { "value": "Hololive EN", "match": "exact" },
      "debutYear": { "value": 2020, "match": "higher", "direction": "↓" },
      "gender": { "value": "女", "match": "exact" },
      "status": { "value": "活动", "match": "exact" },
      "hairColor": { "value": ["蓝", "白"], "match": "partial" },
      "fanName": { "value": "Shrimp", "match": "none" }
    }
  }
}
```

**响应（猜中）：**

```json
{
  "code": 200,
  "data": {
    "isCorrect": true,
    "isGameOver": true,
    "isWin": true,
    "attemptsUsed": 4,
    "targetVtuber": {
      "id": 456,
      "name": "目标VTuber",
      "avatarUrl": "https://..."
    }
  }
}
```

**响应（游戏结束-未猜中）：**

```json
{
  "code": 200,
  "data": {
    "isCorrect": false,
    "isGameOver": true,
    "isWin": false,
    "attemptsUsed": 8,
    "targetVtuber": {
      "id": 456,
      "name": "目标VTuber",
      "avatarUrl": "https://..."
    }
  }
}
```

---

## 三、单人模式 API

### 3.1 获取题库标签列表

```
GET /api/game/single/pools
```

**响应：**

```json
{
  "code": 200,
  "data": [
    {
      "tagName": "全量",
      "description": "所有可用 VTuber",
      "vtuberCount": 152
    },
    {
      "tagName": "日V",
      "description": "日本地区 VTuber",
      "vtuberCount": 89
    }
  ]
}
```

### 3.2 开始单人模式游戏

```
POST /api/game/single/start
```

**请求体：**

```json
{
  "poolTag": "全量"
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "sessionId": "sess_550e8400e29b41d4a716446655440000",
    "maxAttempts": 8,
    "poolTag": "全量",
    "vtuberCount": 152
  }
}
```

> 注意：返回 `sessionId`，后续猜测通过该 ID 关联，不暴露目标信息。

### 3.3 提交单人模式猜测

```
POST /api/game/single/guess
```

**请求体：**

```json
{
  "sessionId": "sess_550e8400e29b41d4a716446655440000",
  "vtuberId": 123
}
```

**响应：** 同每日模式猜测响应格式。

### 3.4 结束单人模式游戏

```
POST /api/game/single/end
```

**请求体：**

```json
{
  "sessionId": "sess_550e8400e29b41d4a716446655440000"
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "sessionId": "sess_...",
    "isWin": true,
    "attemptsUsed": 5,
    "targetVtuber": {
      "id": 456,
      "name": "目标VTuber"
    }
  }
}
```

---

## 四、VTuber 搜索 API

### 4.1 搜索 VTuber

```
GET /api/vtuber/search?keyword=gura&limit=10
```

**请求参数：**

| 参数 | 必填 | 说明 |
|------|------|------|
| keyword | 是 | 搜索关键词 |
| limit | 否 | 返回数量，默认 10，最大 20 |

**响应：**

```json
{
  "code": 200,
  "data": [
    {
      "id": 123,
      "name": "Gawr Gura",
      "nameCn": "噶呜·古拉",
      "nameEn": "Gawr Gura",
      "avatarUrl": "https://...",
      "groupName": "Hololive EN",
      "region": "英语圈"
    }
  ]
}
```

### 4.2 获取 VTuber 详情

```
GET /api/vtuber/{id}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "id": 123,
    "nameCn": "噶呜·古拉",
    "nameEn": "Gawr Gura",
    "nameJp": "がうる・ぐら",
    "aliases": ["Gura", "鲨鲨"],
    "region": "英语圈",
    "groupName": "Hololive EN",
    "debutYear": 2020,
    "activityStatus": "active",
    "gender": "female",
    "hairColor": ["蓝", "白"],
    "fanName": "Shrimp",
    "avatarUrl": "https://..."
  }
}
```

---

## 五、对比规则说明

### 5.1 match 值定义

| 值 | 说明 | 前端样式 |
|----|------|----------|
| exact | 完全匹配 | 绿色高亮 |
| partial | 部分匹配 | 橙色高亮 |
| none | 不匹配 | 灰色 |
| higher | 目标值更高（出道年份） | ↑ 箭头 |
| lower | 目标值更低（出道年份） | ↓ 箭头 |

### 5.2 各属性对比逻辑

| 属性 | 对比方式 | partial 条件 |
|------|----------|--------------|
| region | 完全匹配 | - |
| group | 完全匹配 | 同一母公司不同分部 |
| debutYear | 数值对比 | - |
| gender | 完全匹配 | - |
| activityStatus | 完全匹配 | - |
| hairColor | 数组对比 | 有交集但不完全相同 |
| fanName | 完全匹配 | - |

---

*文档版本：v1.0 | 更新日期：2026-08-18*
