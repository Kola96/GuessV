# M3 手动测试方案

> 本文档供开发者手动验收 M3（游戏核心）使用。
> 自动化测试已通过（18 个新增，累计 54 个），本方案聚焦人工端到端验证。
> 返回 [路线图](../000-roadmap.md)

---

## 一、测试前准备

```bash
git pull
Remove-Item data/guessv.db -Force -ErrorAction SilentlyContinue
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**启动预期**：导入 9769 条 + 种子 10 条 + `Started GuessVApplication`。

所有测试需要先初始化用户拿 token，建议在 PowerShell 中：

```powershell
'{"nickname":"测试玩家","deviceFingerprint":"fp"}' | Set-Content "$env:TEMP\init.json" -Encoding UTF8 -NoNewline
$init = curl.exe -s -X POST http://localhost:8080/api/user/init -H "Content-Type: application/json" --data-binary "@$env:TEMP\init.json" | ConvertFrom-Json
$token = $init.data.token
$h = @{ "X-User-Token" = $token; "Content-Type" = "application/json" }
```

---

## 二、测试用例

### 用例 1：每日模式信息

```powershell
curl.exe -s http://localhost:8080/api/game/daily -H "X-User-Token: $token"
```

**预期**：`code:200`，含 `date`(今天)、`maxAttempts:8`、`totalVtuberCount:10`、`hasPlayed:false`、`guesses:[]`。**不含目标 V 信息**。

---

### 用例 2：每日猜测（猜错）

先搜一个 V 拿到 id：

```powershell
$search = curl.exe -s "http://localhost:8080/api/vtuber/search?keyword=gura&limit=1" | ConvertFrom-Json
$guraId = $search.data[0].id   # 应为 9770
'{"vtuberId":' + $guraId + '}' | Set-Content "$env:TEMP\guess.json" -Encoding UTF8 -NoNewline
curl.exe -s -X POST http://localhost:8080/api/game/daily/guess -H "X-User-Token: $token" -H "Content-Type: application/json" --data-binary "@$env:TEMP\guess.json"
```

**预期**：`code:200`，返回 `comparison` 对象，含 8 个属性对比（name/region/group/debutYear/gender/status/hairColor/fanName）。
- 若 Gura 正好是今日目标 → `correct:true, win:true`（跳到用例 4）
- 否则 → `correct:false`，看对比方向箭头

---

### 用例 3：重复猜测被拒

```powershell
curl.exe -s -X POST http://localhost:8080/api/game/daily/guess -H "X-User-Token: $token" -H "Content-Type: application/json" --data-binary "@$env:TEMP\guess.json"
```

**预期**：`code:409`，message 含"猜过"。

---

### 用例 4：猜中（胜利）

要看到胜利效果，需要猜中今日目标。可换不同 V 多猜几次。或直接查数据库看今日目标：

```powershell
node -e "const{DatabaseSync}=require('node:sqlite');const d=new DatabaseSync('D:/CodeRepo/GuessV/data/guessv.db');const t=d.prepare('SELECT v.name_en FROM daily_target dt JOIN vtuber v ON dt.vtuber_id=v.id WHERE dt.target_date=date(\"now\")').get();console.log('今日目标:',t.name_en)"
```

然后用目标名搜索拿 id，再 guess → 应返回 `correct:true, win:true, targetVtuber` 揭晓。

---

### 用例 5：单人题库列表

```powershell
curl.exe -s "http://localhost:8080/api/game/single/pools" -H "X-User-Token: $token"
```

**预期**：6 个题库（全量:10, 日V:5, 国V:0, 英语圈:5, Hololive:10, Nijisanji:0）。

---

### 用例 6：单人开始

```powershell
'{"poolTag":"全量"}' | Set-Content "$env:TEMP\start.json" -Encoding UTF8 -NoNewline
$start = curl.exe -s -X POST http://localhost:8080/api/game/single/start -H "X-User-Token: $token" -H "Content-Type: application/json" --data-binary "@$env:TEMP\start.json" | ConvertFrom-Json
$sessionId = $start.data.sessionId
```

**预期**：返回 `sessionId`、`maxAttempts:8`、`poolTag:"全量"`、`vtuberCount:10`。

---

### 用例 7：单人猜测

```powershell
'{"sessionId":' + $sessionId + ',"vtuberId":' + $guraId + '}' | Set-Content "$env:TEMP\sguess.json" -Encoding UTF8 -NoNewline
curl.exe -s -X POST http://localhost:8080/api/game/single/guess -H "X-User-Token: $token" -H "Content-Type: application/json" --data-binary "@$env:TEMP\sguess.json"
```

**预期**：`code:200`，返回 comparison。猜中则 `win:true`。

---

### 用例 8：单人断线恢复

```powershell
curl.exe -s "http://localhost:8080/api/game/single/$sessionId" -H "X-User-Token: $token"
```

**预期**：返回当前局状态（已猜次数、历史猜测列表）。

---

### 用例 9：单人无限重开

再次调用 `/single/start`，应返回新的 `sessionId`，互不影响。

---

### 用例 10：越权访问被拒

用另一个用户的 token 访问别人的 sessionId → `code:403`。

---

### 用例 11：对比规则验证

检查返回的 comparison 字段是否符合规则：

| 属性 | 检查点 |
|------|--------|
| group | 同公司不同分部（Hololive vs Hololive EN）→ partial |
| debutYear | 猜2018/目标2020 → higher+↑；反向 → lower+↓ |
| hairColor | 数组有交集但不同 → partial |
| gender/status | 翻译为中文（女/活动/毕业等） |

---

### 用例 12：自动化测试回归

```bash
cd backend && mvn test
```

**预期**：`Tests run: 54, Failures: 0, Errors: 0` + `BUILD SUCCESS`

---

## 三、测试结果记录

| 用例 | 结果（✅/❌） | 备注 |
|------|--------------|------|
| 1. 每日信息 | | |
| 2. 每日猜错 | | |
| 3. 重复猜被拒 | | |
| 4. 猜中胜利 | | |
| 5. 单人题库 | | |
| 6. 单人开始 | | |
| 7. 单人猜测 | | |
| 8. 断线恢复 | | |
| 9. 无限重开 | | |
| 10. 越权被拒 | | |
| 11. 对比规则 | | |
| 12. 自动化回归 | | |

---

*验收通过后，M3 正式关闭，进入 M4（前端游戏界面）。*
