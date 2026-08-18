# 用户 API 设计

> 本文档定义用户相关的 REST API 接口规范。
> 返回 [AGENTS.md](../../AGENTS.md)

---

## 一、通用规范

### 1.1 基础路径

```
/api/user
```

### 1.2 请求头

| 头部 | 必填 | 说明 |
|------|------|------|
| Content-Type | 是 | application/json |
| X-User-Token | 可选 | 用户凭证（部分接口需要） |

---

## 二、用户初始化 API

### 2.1 创建匿名用户

```
POST /api/user/init
```

**请求体：**

```json
{
  "nickname": "小明",
  "deviceFingerprint": "fp_abc123..."
}
```

**或使用随机昵称：**

```json
{
  "useRandomNickname": true,
  "deviceFingerprint": "fp_abc123..."
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "nickname": "小明",
    "gameId": "AB12",
    "displayName": "小明#AB12",
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "isAnonymous": true,
    "createdAt": "2026-08-18T10:00:00Z"
  }
}
```

**字段说明：**

| 字段 | 说明 |
|------|------|
| userId | 用户 UUID，内部标识 |
| nickname | 用户昵称 |
| gameId | 4 位游戏 ID |
| displayName | 显示名称（昵称#ID） |
| token | 用户凭证，用于后续请求 |
| isAnonymous | 是否匿名用户 |

**错误响应：**

```json
{
  "code": 400,
  "message": "昵称包含敏感词，请更换"
}
```

---

### 2.2 获取随机昵称建议

```
GET /api/user/nickname/random?count=5
```

**响应：**

```json
{
  "code": 200,
  "data": [
    "快乐的小猫咪",
    "单推人",
    "DD头子",
    "软糖",
    "霸主"
  ]
}
```

---

### 2.3 检查昵称可用性

```
POST /api/user/nickname/check
```

**请求体：**

```json
{
  "nickname": "小明"
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "available": true,
    "filteredNickname": "小明",
    "hasSensitiveWord": false
  }
}
```

**字段说明：**

| 字段 | 说明 |
|------|------|
| available | 是否可用 |
| filteredNickname | 过滤后的昵称（敏感词替换为 *） |
| hasSensitiveWord | 是否包含敏感词 |

---

## 三、用户认证 API

### 3.1 绑定账号

```
POST /api/user/bind
```

**请求头：**

```
X-User-Token: eyJhbGciOiJIUzI1NiIs...
```

**请求体：**

```json
{
  "username": "xiaoming",
  "password": "securePassword123",
  "email": "xiaoming@example.com"
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "xiaoming",
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 86400
  }
}
```

**错误响应：**

```json
{
  "code": 409,
  "message": "用户名已被使用"
}
```

---

### 3.2 账号登录

```
POST /api/user/login
```

**请求体：**

```json
{
  "username": "xiaoming",
  "password": "securePassword123"
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "nickname": "小明",
    "gameId": "AB12",
    "displayName": "小明#AB12",
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 86400
  }
}
```

---

### 3.3 刷新 Token

```
POST /api/user/refresh
```

**请求体：**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 86400
  }
}
```

---

## 四、用户信息 API

### 4.1 获取当前用户信息

```
GET /api/user/profile
```

**请求头：**

```
X-User-Token: eyJhbGciOiJIUzI1NiIs...
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "nickname": "小明",
    "gameId": "AB12",
    "displayName": "小明#AB12",
    "isAnonymous": false,
    "username": "xiaoming",
    "email": "xiaoming@example.com",
    "avatarUrl": null,
    "createdAt": "2026-08-18T10:00:00Z",
    "lastActiveAt": "2026-08-18T15:30:00Z"
  }
}
```

---

### 4.2 修改昵称

```
PUT /api/user/nickname
```

**请求头：**

```
X-User-Token: eyJhbGciOiJIUzI1NiIs...
```

**请求体：**

```json
{
  "nickname": "新昵称"
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "nickname": "新昵称",
    "displayName": "新昵称#AB12",
    "changedAt": "2026-08-18T16:00:00Z"
  }
}
```

---

## 五、用户统计 API

### 5.1 获取个人统计

```
GET /api/user/stats
```

**请求头：**

```
X-User-Token: eyJhbGciOiJIUzI1NiIs...
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "totalGames": 42,
    "totalWins": 35,
    "winRate": 0.833,
    "currentStreak": 5,
    "maxStreak": 12,
    "avgAttempts": 4.2,
    "dailyHistory": [
      {
        "date": "2026-08-18",
        "isWin": true,
        "attempts": 4
      }
    ]
  }
}
```

---

### 5.2 获取游戏历史

```
GET /api/user/history?page=1&size=20&mode=daily
```

**请求参数：**

| 参数 | 必填 | 说明 |
|------|------|------|
| page | 否 | 页码，默认 1 |
| size | 否 | 每页数量，默认 20 |
| mode | 否 | 游戏模式筛选 |

**响应：**

```json
{
  "code": 200,
  "data": {
    "total": 42,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 1001,
        "mode": "daily",
        "targetName": "目标VTuber",
        "isWin": true,
        "attempts": 4,
        "maxAttempts": 8,
        "startedAt": "2026-08-18T10:00:00Z"
      }
    ]
  }
}
```

---

## 六、排行榜 API（预留）

### 6.1 获取日榜

```
GET /api/leaderboard/daily?date=2026-08-18&limit=100
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "date": "2026-08-18",
    "myRank": 15,
    "totalPlayers": 1523,
    "rankings": [
      {
        "rank": 1,
        "displayName": "玩家#AB12",
        "attempts": 3,
        "isWin": true
      }
    ]
  }
}
```

---

### 6.2 获取周榜

```
GET /api/leaderboard/weekly?week=2026-W34&limit=100
```

---

*文档版本：v1.0 | 更新日期：2026-08-18*
