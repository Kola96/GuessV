# 属性对比规则

> 本文档详细定义猜测时各属性的对比逻辑和提示规则。
> 返回 [AGENTS.md](../../AGENTS.md)

---

## 一、对比结果类型

| 类型 | 值 | 说明 | 前端样式 |
|------|-----|------|----------|
| 完全匹配 | exact | 属性值完全相同 | 绿色高亮 ✓ |
| 部分匹配 | partial | 属性值部分重叠 | 橙色高亮 ◐ |
| 不匹配 | none | 属性值完全不同 | 灰色 ✗ |
| 数值更高 | higher | 目标值比猜测值高 | ↑ 箭头 |
| 数值更低 | lower | 目标值比猜测值低 | ↓ 箭头 |

---

## 二、各属性对比规则

### 2.1 所属地区（region）

| 对比方式 | 完全匹配 |
|----------|----------|
| 示例 | 猜测"日本" vs 目标"日本" → exact |

**无 partial 情况**

---

### 2.2 所属团体（group）

| 对比方式 | 完全匹配 |
|----------|----------|
| partial 条件 | 同一母公司不同分部 |

**partial 示例：**
- 猜测 "Hololive" vs 目标 "Hololive EN" → partial
- 猜测 "Nijisanji" vs 目标 "Nijisanji EN" → partial

**匹配逻辑：**
```java
if (guessGroup.equals(targetGroup)) return "exact";
if (isSameCompany(guessGroup, targetGroup)) return "partial";
return "none";
```

---

### 2.3 出道年份（debutYear）

| 对比方式 | 数值对比 |
|----------|----------|
| 返回值 | higher / lower / exact |

**示例：**
- 猜测 2020 年 vs 目标 2022 年 → higher（↑）
- 猜测 2023 年 vs 目标 2021 年 → lower（↓）
- 猜测 2020 年 vs 目标 2020 年 → exact

---

### 2.4 性别（gender）

| 对比方式 | 完全匹配 |
|----------|----------|
| 可选值 | male / female / other |

**无 partial 情况**

---

### 2.5 活动状态（activityStatus）

| 对比方式 | 完全匹配 |
|----------|----------|
| 可选值 | active / graduated / hiatus / suspended |

**无 partial 情况**

---

### 2.6 发色（hairColor）

| 对比方式 | 数组对比 |
|----------|----------|
| exact | 两个数组完全相同（忽略顺序） |
| partial | 两个数组有交集但不完全相同 |
| none | 两个数组无交集 |

**示例：**
- 猜测 ["蓝", "白"] vs 目标 ["蓝", "白"] → exact
- 猜测 ["蓝"] vs 目标 ["蓝", "白"] → partial
- 猜测 ["红"] vs 目标 ["蓝", "白"] → none

**匹配逻辑：**
```java
Set<String> guessSet = new HashSet<>(guessColors);
Set<String> targetSet = new HashSet<>(targetColors);

if (guessSet.equals(targetSet)) return "exact";
if (!Collections.disjoint(guessSet, targetSet)) return "partial";
return "none";
```

---

### 2.7 粉丝名（fanName）

| 对比方式 | 完全匹配 |
|----------|----------|
| 说明 | 粉丝群体称呼，如 "Shrimp"、"Chumbuds" |

**无 partial 情况**

---

## 三、响应格式

### 3.1 完整对比响应

```json
{
  "name": { "value": "Gawr Gura", "match": "exact" },
  "region": { "value": "英语圈", "match": "exact" },
  "group": { "value": "Hololive EN", "match": "partial" },
  "debutYear": { "value": 2020, "match": "higher", "direction": "↓" },
  "gender": { "value": "女", "match": "exact" },
  "status": { "value": "活动", "match": "exact" },
  "hairColor": { "value": ["蓝", "白"], "match": "partial" },
  "fanName": { "value": "Shrimp", "match": "none" }
}
```

### 3.2 字段说明

| 字段 | 说明 |
|------|------|
| value | 猜测 VTuber 的属性值（展示用） |
| match | 对比结果类型 |
| direction | 仅数值类型返回，指示目标方向 |

---

## 四、特殊处理规则

### 4.1 空值处理

| 场景 | 处理方式 |
|------|----------|
| 猜测 VTuber 某字段为空 | 该属性返回 match: "none" |
| 目标 VTuber 某字段为空 | 该属性返回 match: "none" |
| 双方均为空 | 该属性返回 match: "exact" |

### 4.2 多语言名称

- 名称对比使用所有可用名称（cn/en/jp/aliases）
- 任意一个名称匹配即视为 exact

### 4.3 大小写处理

- 所有字符串对比忽略大小写
- 首尾空格自动去除

---

## 五、前端展示建议

| match 值 | 背景色 | 文字色 | 图标 |
|----------|--------|--------|------|
| exact | #4CAF50 | 白色 | ✓ |
| partial | #F2994A | 白色 | ◐ |
| none | #4F4F4F | 白色 | ✗ |
| higher | #4F4F4F | 白色 | ↑ |
| lower | #4F4F4F | 白色 | ↓ |

---

*文档版本：v1.0 | 更新日期：2026-08-18*
