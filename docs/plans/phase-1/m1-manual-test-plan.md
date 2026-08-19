# M1 手动测试方案

> 本文档供开发者手动验收 M1（后端骨架与数据导入）使用。
> 所有自动化测试已通过（13 个），本方案聚焦**人工端到端验证**。
> 返回 [路线图](../000-roadmap.md)

---

## 一、测试环境要求

| 项目 | 要求 |
|------|------|
| JDK | 21+（`java -version` 验证） |
| Maven | 3.9+（`mvn -version` 验证） |
| 网络 | 首次构建需下载 Maven 依赖 |
| 端口 | 8080 未被占用 |

---

## 二、测试前准备

```bash
# 1. 拉取最新代码
git pull

# 2. 删除旧数据库（模拟全新环境）
# Windows PowerShell:
Remove-Item data/guessv.db -Force -ErrorAction SilentlyContinue
# Linux/macOS:
rm -f data/guessv.db

# 3. 启动后端（在 backend/ 目录下）
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**启动预期**：日志中依次出现（中文）：
1. `解析到 9769 条 VTuber（过滤后）`
2. `团体导入完成，共 212 个`
3. `导入完成，成功 9769 条`
4. `种子数据已插入 10 条`
5. `Started GuessVApplication`

> 首次启动导入约 30 秒。之后启动因数据已存在会显示「跳过导入」。

---

## 三、测试用例

### 用例 1：健康检查

```bash
curl http://localhost:8080/api/health
```

**预期**：
```json
{"code":200,"message":"success","data":{"status":"UP","app":"GuessV"}}
```

---

### 用例 2：英文关键词搜索

```bash
curl "http://localhost:8080/api/vtuber/search?keyword=gura&limit=5"
```

**预期**：`code: 200`，data 数组包含：
```json
{"name":"噶呜·古拉","nameEn":"Gawr Gura","groupName":"Hololive EN","region":"英语圈"}
```

---

### 用例 3：中文关键词搜索

```bash
curl "http://localhost:8080/api/vtuber/search?keyword=古拉&limit=5"
```

**预期**：`code: 200`，结果包含「噶呜·古拉」。

> Windows curl 中文乱码时，用 URL 编码：`%E5%8F%A4%E6%8B%89`（古拉）

---

### 用例 4：日文关键词搜索

```bash
curl "http://localhost:8080/api/vtuber/search?keyword=みこ&limit=5"
```

**预期**：`code: 200`，结果包含「樱巫女 / Sakura Miko」。

---

### 用例 5：空关键词校验

```bash
curl "http://localhost:8080/api/vtuber/search?keyword="
```

**预期**：
```json
{"code":400,"message":"搜索关键词不能为空"}
```

---

### 用例 6：raw 状态数据不可搜（防泄露）

种子数据中有「--晴朗蓝--」是 raw 状态（来自导入），不应被搜到：

```bash
curl "http://localhost:8080/api/vtuber/search?keyword=晴朗蓝&limit=5"
```

**预期**：`code: 200`，但 `data` 为 **空数组 `[]`**（raw 状态数据被过滤）。

> URL 编码备用：`%E6%99%B4%E6%9C%97%E8%93%9D`（晴朗蓝）

---

### 用例 7：种子数据完整性

以下 10 位 VTuber 都应能搜到（分别验证中英文均可）：

| 中文名 | 英文名 | 搜索关键词建议 |
|--------|--------|----------------|
| 噶呜·古拉 | Gawr Gura | gura |
| 森美声 | Mori Calliope | calli |
| 小鸟游琪亚拉 | Takanashi Kiara | kiara |
| 亚美·华生 | Amelia Watson | amelia |
| 一伊那尓栖 | Ninomae Ina'nis | ina |
| 白上吹雪 | Shirakami Fubuki | fubuki |
| 兔田佩克拉 | Usada Pekora | pekora |
| 樱巫女 | Sakura Miko | miko |
| 凑阿库娅 | Minato Aqua | aqua |
| 紫咲诗音 | Murasaki Shion | shion |

---

### 用例 8：重复启动幂等性

1. Ctrl+C 停止后端
2. 再次执行 `mvn spring-boot:run -Dspring-boot.run.profiles=dev`

**预期**：日志显示「VTuber 表非空，跳过导入」和「已有 10 条 active 数据，跳过种子数据」，**不会重复导入**。

---

### 用例 9：自动化测试回归

```bash
cd backend
mvn test
```

**预期**：`Tests run: 13, Failures: 0, Errors: 0` + `BUILD SUCCESS`

---

## 四、测试结果记录

| 用例 | 结果（✅/❌） | 备注 |
|------|--------------|------|
| 1. 健康检查 | | |
| 2. 英文搜索 | | |
| 3. 中文搜索 | | |
| 4. 日文搜索 | | |
| 5. 空关键词 | | |
| 6. raw 过滤 | | |
| 7. 种子完整性 | | |
| 8. 幂等启动 | | |
| 9. 自动化回归 | | |

---

## 五、问题反馈

如发现问题，请记录：
1. 用例编号
2. 实际结果（截图/日志）
3. 预期结果
4. 操作系统 + JDK 版本

反馈给开发 Agent 时附上 `backend/target/` 下最新日志。

---

*验收通过后，M1 正式关闭，进入 M2（用户系统）。*
