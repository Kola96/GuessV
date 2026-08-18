# 运营后台 API 设计

> 本文档定义运营后台相关的 REST API 接口规范。
> 返回 [AGENTS.md](../../AGENTS.md)

---

## 一、通用规范

### 1.1 基础路径

```
/api/admin
```

### 1.2 请求头

| 头部 | 必填 | 说明 |
|------|------|------|
| Content-Type | 是 | application/json |
| Authorization | 是 | Bearer {admin_token} |

### 1.3 权限说明

| 角色 | 权限 |
|------|------|
| admin | 所有权限 |
| operator | VTuber 管理、爬虫监控 |

---

## 二、认证 API

### 2.1 管理员登录

```
POST /api/admin/login
```

**请求体：**

```json
{
  "username": "admin",
  "password": "adminPassword123"
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 86400,
    "role": "admin"
  }
}
```

---

## 三、VTuber 管理 API

### 3.1 获取 VTuber 列表

```
GET /api/admin/vtuber?page=1&size=20&status=raw&keyword=gura
```

**请求参数：**

| 参数 | 必填 | 说明 |
|------|------|------|
| page | 否 | 页码，默认 1 |
| size | 否 | 每页数量，默认 20 |
| status | 否 | 数据状态筛选：raw/candidate/active/verified |
| keyword | 否 | 搜索关键词 |

**响应：**

```json
{
  "code": 200,
  "data": {
    "total": 10014,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 123,
        "uuid": "e3132f27-7b99-5983-9224-e68475e3ffac",
        "nameCn": "噶呜·古拉",
        "nameEn": "Gawr Gura",
        "groupName": "Hololive EN",
        "dataStatus": "active",
        "dataSource": "crawler",
        "lockedFields": ["hair_color"],
        "updatedAt": "2026-08-18T10:00:00Z"
      }
    ]
  }
}
```

---

### 3.2 获取 VTuber 详情（含所有字段）

```
GET /api/admin/vtuber/{id}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "id": 123,
    "uuid": "e3132f27-7b99-5983-9224-e68475e3ffac",
    "nameCn": "噶呜·古拉",
    "nameEn": "Gawr Gura",
    "nameJp": "がうる・ぐら",
    "aliases": ["Gura", "鲨鲨"],
    "debutYear": 2020,
    "debutDate": "2020-09-13",
    "region": "英语圈",
    "groupId": 5,
    "groupName": "Hololive EN",
    "activityStatus": "active",
    "gender": "female",
    "hairColor": ["蓝", "白"],
    "eyeColor": ["蓝"],
    "fanName": "Shrimp",
    "representativeColor": "#1E90FF",
    "platforms": ["YouTube", "Twitter"],
    "languages": ["英语", "日语"],
    "avatarUrl": "https://...",
    "dataStatus": "active",
    "dataSource": "crawler",
    "lockedFields": ["hair_color"],
    "createdAt": "2026-08-18T09:00:00Z",
    "updatedAt": "2026-08-18T10:00:00Z"
  }
}
```

---

### 3.3 提升 VTuber 状态

```
POST /api/admin/vtuber/{id}/promote
```

**请求体：**

```json
{
  "targetStatus": "candidate"
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "id": 123,
    "previousStatus": "raw",
    "currentStatus": "candidate",
    "promotedAt": "2026-08-18T16:00:00Z"
  }
}
```

---

### 3.4 编辑 VTuber 属性

```
PUT /api/admin/vtuber/{id}/edit
```

**请求体：**

```json
{
  "fields": {
    "hairColor": ["蓝", "白"],
    "fanName": "Shrimp"
  },
  "lockFields": true
}
```

**参数说明：**

| 参数 | 说明 |
|------|------|
| fields | 要修改的字段和值 |
| lockFields | 是否锁定这些字段（默认 true） |

**响应：**

```json
{
  "code": 200,
  "data": {
    "id": 123,
    "updatedFields": ["hairColor", "fanName"],
    "lockedFields": ["hairColor", "fanName"],
    "dataSource": "manual",
    "updatedAt": "2026-08-18T16:00:00Z"
  }
}
```

---

### 3.5 解锁字段

```
POST /api/admin/vtuber/{id}/unlock
```

**请求体：**

```json
{
  "fields": ["hairColor"]
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "id": 123,
    "unlockedFields": ["hairColor"],
    "remainingLockedFields": ["fanName"]
  }
}
```

---

### 3.6 标记为已核实

```
POST /api/admin/vtuber/{id}/verify
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "id": 123,
    "dataStatus": "verified",
    "verifiedAt": "2026-08-18T16:00:00Z"
  }
}
```

---

## 四、爬虫管理 API

### 4.1 获取爬虫状态

```
GET /api/admin/crawler/status
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "isRunning": false,
    "lastRunAt": "2026-08-18T02:00:00Z",
    "nextRunAt": "2026-08-19T02:00:00Z",
    "pendingCount": 45,
    "successCount": 120,
    "failedCount": 3,
    "recentLogs": [
      {
        "vtuberId": 123,
        "vtuberName": "噶呜·古拉",
        "status": "success",
        "fieldsUpdated": ["hairColor", "fanName"],
        "runAt": "2026-08-18T02:15:00Z"
      }
    ]
  }
}
```

---

### 4.2 手动触发爬虫

```
POST /api/admin/crawler/trigger
```

**请求体（可选）：**

```json
{
  "vtuberIds": [123, 456]
}
```

> 不传 `vtuberIds` 则处理全部 candidate 状态的 VTuber。

**响应：**

```json
{
  "code": 200,
  "data": {
    "targetCount": 2,
    "message": "爬取任务已启动（异步执行，请稍后查看状态）"
  }
}
```

---

## 五、每日目标管理 API

### 5.1 获取每日目标历史

```
GET /api/admin/daily-target?page=1&size=30
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "total": 30,
    "records": [
      {
        "date": "2026-08-18",
        "vtuberId": 456,
        "vtuberName": "目标VTuber",
        "createdAt": "2026-08-18T00:00:00Z"
      }
    ]
  }
}
```

---

### 5.2 手动设置每日目标（紧急情况）

```
POST /api/admin/daily-target
```

**请求体：**

```json
{
  "date": "2026-08-19",
  "vtuberId": 789
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "date": "2026-08-19",
    "vtuberId": 789,
    "vtuberName": "手动设置的目标",
    "message": "每日目标已更新"
  }
}
```

---

## 六、题库管理 API

### 6.1 获取题库标签列表

```
GET /api/admin/pool-tag
```

**响应：**

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "tagName": "全量",
      "description": "所有可用 VTuber",
      "filterRule": {"data_status": ["active", "verified"]},
      "vtuberCount": 152,
      "isActive": true
    }
  ]
}
```

---

### 6.2 创建/更新题库标签

```
POST /api/admin/pool-tag
PUT /api/admin/pool-tag/{id}
```

**请求体：**

```json
{
  "tagName": "热门",
  "description": "高知名度 VTuber",
  "filterRule": {
    "data_status": ["verified"],
    "min_subscriber": 100000
  },
  "isActive": true,
  "sortOrder": 10
}
```

---

## 七、操作日志 API

### 7.1 获取操作日志

```
GET /api/admin/operation-log?page=1&size=50&targetType=vtuber&targetId=123
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "total": 25,
    "records": [
      {
        "id": 1001,
        "operatorName": "admin",
        "operationType": "edit_vtuber",
        "targetType": "vtuber",
        "targetId": 123,
        "targetName": "噶呜·古拉",
        "fieldName": "hairColor",
        "oldValue": "[\"蓝\"]",
        "newValue": "[\"蓝\", \"白\"]",
        "ipAddress": "192.168.1.1",
        "createdAt": "2026-08-18T16:00:00Z"
      }
    ]
  }
}
```

---

## 八、数据看板 API

### 8.1 获取概览数据

```
GET /api/admin/dashboard
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "vtuberStats": {
      "raw": 9800,
      "candidate": 45,
      "active": 150,
      "verified": 19
    },
    "todayStats": {
      "date": "2026-08-18",
      "totalPlayers": 1523,
      "totalGames": 1689,
      "winRate": 0.72
    },
    "crawlerStats": {
      "pending": 45,
      "successToday": 120,
      "failedToday": 3
    }
  }
}
```

---

*文档版本：v1.0 | 更新日期：2026-08-18*
