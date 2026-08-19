# M2 手动测试方案

> 本文档供开发者手动验收 M2（用户系统）使用。
> 自动化测试已通过（23 个新增，累计 36 个），本方案聚焦人工端到端验证。
> 返回 [路线图](../000-roadmap.md)

---

## 一、测试前准备

```bash
git pull
Remove-Item data/guessv.db -Force -ErrorAction SilentlyContinue   # 全新环境
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**启动预期**：日志显示导入 9769 条 + 种子 10 条 + `Started GuessVApplication`。

> Windows curl 中文需用 URL 编码或 `--data-binary @file` 传 JSON 体。下文给出关键 URL 编码。

---

## 二、测试用例

### 用例 1：随机昵称

```bash
curl "http://localhost:8080/api/user/nickname/random?count=5"
```

**预期**：`code: 200`，返回 5 个昵称（如 "单推人"、"快乐的观测者" 等）。

---

### 用例 2：用户初始化（自定义昵称）

Windows 下用文件传 JSON 避免 curl 转义问题：

```powershell
'{"nickname":"测试小明","deviceFingerprint":"fp_test"}' | Set-Content "$env:TEMP\init.json" -Encoding UTF8 -NoNewline
curl.exe -s -X POST http://localhost:8080/api/user/init -H "Content-Type: application/json" --data-binary "@$env:TEMP\init.json"
```

**预期**：
```json
{"code":200,"data":{
  "userId":"<UUID>",
  "nickname":"测试小明",
  "gameId":"<4位ID>",
  "displayName":"测试小明#<4位ID>",
  "token":"<JWT>",
  "isAnonymous":true
}}
```

**记录返回的 token**，后续用例需要。

---

### 用例 3：用户初始化（随机昵称）

```powershell
'{"useRandomNickname":true,"deviceFingerprint":"fp2"}' | Set-Content "$env:TEMP\init2.json" -Encoding UTF8 -NoNewline
curl.exe -s -X POST http://localhost:8080/api/user/init -H "Content-Type: application/json" --data-binary "@$env:TEMP\init2.json"
```

**预期**：返回随机昵称 + gameId + token。

---

### 用例 4：初始化-敏感词昵称被拒

```powershell
'{"nickname":"赌博大王","deviceFingerprint":"fp3"}' | Set-Content "$env:TEMP\bad.json" -Encoding UTF8 -NoNewline
curl.exe -s -X POST http://localhost:8080/api/user/init -H "Content-Type: application/json" --data-binary "@$env:TEMP\bad.json"
```

**预期**：`code: 400`，message 含"敏感词"。

---

### 用例 5：初始化-昵称过长被拒

```powershell
'{"nickname":"这是一个超过十六个字符的昵称真的太长了","deviceFingerprint":"fp4"}' | Set-Content "$env:TEMP\long.json" -Encoding UTF8 -NoNewline
curl.exe -s -X POST http://localhost:8080/api/user/init -H "Content-Type: application/json" --data-binary "@$env:TEMP\long.json"
```

**预期**：`code: 400`，message 含"长度"。

---

### 用例 6：个人信息-带 token

```bash
curl http://localhost:8080/api/user/profile -H "X-User-Token: <用例2的token>"
```

**预期**：`code: 200`，返回当前用户信息（nickname、gameId、displayName、isAnonymous:true）。

---

### 用例 7：个人信息-无 token

```bash
curl http://localhost:8080/api/user/profile
```

**预期**：`code: 401`，message "未提供用户凭证"。

---

### 用例 8：个人信息-无效 token

```bash
curl http://localhost:8080/api/user/profile -H "X-User-Token: invalid.token.here"
```

**预期**：`code: 401`，message 含"无效"或"已过期"。

---

### 用例 9：昵称校验-敏感词

```bash
# 赌博王 = %E8%B5%8C%E5%8D%9A%E7%8E%8B
curl "http://localhost:8080/api/user/nickname/check?nickname=%E8%B5%8C%E5%8D%9A%E7%8E%8B"
```

**预期**：`valid: false`，`reason: "sensitive"`。

---

### 用例 10：昵称校验-合法

```bash
# 小明 = %E5%B0%8F%E6%98%8E
curl "http://localhost:8080/api/user/nickname/check?nickname=%E5%B0%8F%E6%98%8E"
```

**预期**：`valid: true`。

---

### 用例 11：修改昵称-成功

```powershell
'{"nickname":"新昵称威武"}' | Set-Content "$env:TEMP\rename.json" -Encoding UTF8 -NoNewline
curl.exe -s -X PUT http://localhost:8080/api/user/nickname -H "Content-Type: application/json" -H "X-User-Token: <用例2的token>" --data-binary "@$env:TEMP\rename.json"
```

**预期**：`code: 200`，nickname 变为"新昵称威武"，displayName 对应更新。

---

### 用例 12：修改昵称-敏感词被拒

```powershell
'{"nickname":"色情主播"}' | Set-Content "$env:TEMP\badrename.json" -Encoding UTF8 -NoNewline
curl.exe -s -X PUT http://localhost:8080/api/user/nickname -H "Content-Type: application/json" -H "X-User-Token: <用例2的token>" --data-binary "@$env:TEMP\badrename.json"
```

**预期**：`code: 400`，message 含"敏感词"。

---

### 用例 13：游戏 ID 唯一性

连续初始化 5 个用户，检查 gameId 是否各不相同：

```powershell
1..5 | ForEach-Object {
  '{"useRandomNickname":true,"deviceFingerprint":"fp"}' | Set-Content "$env:TEMP\i.json" -Encoding UTF8 -NoNewline
  (curl.exe -s -X POST http://localhost:8080/api/user/init -H "Content-Type: application/json" --data-binary "@$env:TEMP\i.json" | ConvertFrom-Json).data.gameId
}
```

**预期**：5 个 gameId 互不重复。

---

### 用例 14：自动化测试回归

```bash
cd backend && mvn test
```

**预期**：`Tests run: 36, Failures: 0, Errors: 0` + `BUILD SUCCESS`

---

## 三、测试结果记录

| 用例 | 结果（✅/❌） | 备注 |
|------|--------------|------|
| 1. 随机昵称 | | |
| 2. 初始化-自定义 | | |
| 3. 初始化-随机 | | |
| 4. 初始化-敏感词 | | |
| 5. 初始化-过长 | | |
| 6. profile-带token | | |
| 7. profile-无token | | |
| 8. profile-无效token | | |
| 9. 校验-敏感词 | | |
| 10. 校验-合法 | | |
| 11. 修改昵称-成功 | | |
| 12. 修改昵称-敏感词 | | |
| 13. gameId 唯一 | | |
| 14. 自动化回归 | | |

---

*验收通过后，M2 正式关闭，进入 M3（游戏核心）。*
