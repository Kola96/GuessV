# 爬虫系统设计

> 本文档定义数据补全爬虫的实现方案。
> 返回 [AGENTS.md](../../AGENTS.md)

---

## 一、设计原则

**这只是一个数据补全工具，不是大型爬虫系统。**

| 原则 | 说明 |
|------|------|
| 简单优先 | Spring Boot 定时任务直接干，不搞独立服务 |
| 低频运行 | 每日一次 + 手动触发，无并发需求 |
| 可控限速 | 单线程串行，请求间隔 2-5 秒 |
| 失败容忍 | 单个失败不影响整体，记录日志即可 |

---

## 二、技术方案

### 2.1 技术选型

| 组件 | 技术 | 说明 |
|------|------|------|
| 定时调度 | Spring `@Scheduled` | 框架自带，零额外依赖 |
| HTTP 客户端 | OkHttp / Hutool HttpUtil | 简单 HTTP 请求 |
| JSON 解析 | Jackson | Spring Boot 自带 |
| HTML 解析 | Jsoup | 兜底使用（优先用 API） |

### 2.2 为什么够用

| 数据来源 | 获取方式 | 是否需要 HTML 解析 |
|----------|----------|--------------------|
| 萌娘百科 | **MediaWiki API（JSON）** | ❌ 不需要 |
| Hololive 官网 | 官方页面 | ✅ 需要，Jsoup 解析 |
| Nijisanji 官网 | 官方页面 | ✅ 需要，Jsoup 解析 |

**萌娘百科 MediaWiki API 示例：**

```
GET https://zh.moegirl.org.cn/api.php?action=parse&page=噶呜·古拉&format=json&prop=wikitext
```

返回 JSON，直接从 wikitext 中提取信息框（Infobox）字段即可。

---

## 三、架构设计

### 3.1 模块位置

```
backend/src/main/java/com/guessv/
├── scheduler/
│   └── CrawlerScheduler.java        # 定时任务入口
├── service/
│   └── crawler/
│       ├── CrawlerService.java      # 爬虫主逻辑
│       ├── MoegirlFetcher.java      # 萌娘百科数据源
│       ├── OfficialSiteFetcher.java # 官网数据源
│       └── DataMerger.java          # 数据合并（跳过锁定字段）
└── entity/
    └── CrawlLog.java                # 爬取日志
```

### 3.2 执行流程

```
每日 02:00 定时触发（或手动触发）
    ↓
查询 candidate 状态的 VTuber 列表
    ↓
逐个处理（单线程串行）：
    ├─ 调用萌娘百科 API 获取数据
    ├─ （可选）调用官网页面补充
    ├─ 合并数据：跳过 locked_fields
    ├─ 更新数据库，data_source = crawler
    ├─ 记录爬取日志
    └─ 休眠 2-5 秒（防封禁）
    ↓
全部完成后更新 VTuber 状态：candidate → active
    ↓
输出汇总日志
```

### 3.3 核心代码结构

```java
@Component
public class CrawlerScheduler {

    @Scheduled(cron = "0 0 2 * * ?")  // 每日凌晨 2:00
    public void dailyCrawl() {
        crawlerService.crawlCandidates();
    }
}

@Service
public class CrawlerService {

    public void crawlCandidates() {
        List<Vtuber> candidates = vtuberMapper.selectCandidates();
        for (Vtuber vtb : candidates) {
            try {
                CrawlResult result = moegirlFetcher.fetch(vtb);
                dataMerger.merge(vtb, result);  // 跳过锁定字段
                Thread.sleep(RandomUtil.randomInt(2000, 5000));
            } catch (Exception e) {
                crawlLogMapper.fail(vtb.getId(), e.getMessage());
            }
        }
    }
}
```

---

## 四、数据合并规则

### 4.1 锁定字段保护

```java
public void merge(Vtuber vtb, CrawlResult result) {
    List<String> locked = vtb.getLockedFields();
    
    result.getFields().forEach((field, value) -> {
        if (!locked.contains(field) && value != null) {
            vtb.setField(field, value);
        }
    });
    
    vtb.setDataSource("crawler");
    vtb.setDataStatus("active");  // candidate → active
}
```

### 4.2 字段映射（萌娘百科 Infobox）

| 萌娘百科字段 | 数据库字段 | 转换规则 |
|--------------|-----------|----------|
| 发色 | hairColor | 拆分为数组，颜色标准化 |
| 瞳色 | eyeColor | 拆分为数组，颜色标准化 |
| 出身地区 | region | 映射到地区枚举 |
| 所属团体 | groupName | 匹配团体表 |
| 出道日期 | debutDate | 解析日期格式 |
| 粉丝名 | fanName | 直接使用 |

---

## 五、手动触发

运营后台提供手动触发入口，内部直接调用 Service：

```
POST /api/admin/crawler/trigger
{ "vtuberIds": [123, 456] }   // 可选，不传则处理全部 candidate
```

后端实现：

```java
@PostMapping("/crawler/trigger")
public Result triggerCrawl(@RequestBody(required = false) TriggerRequest req) {
    // 异步执行，立即返回
    CompletableFuture.runAsync(() -> crawlerService.crawl(req.getVtuberIds()));
    return Result.ok("爬取任务已启动");
}
```

---

## 六、日志与监控

### 6.1 爬取日志表（crawl_log）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| vtuber_id | BIGINT | VTuber ID |
| source | VARCHAR(50) | 数据来源：moegirl/official |
| status | VARCHAR(20) | 结果：success/failed/skipped |
| fields_updated | JSON | 更新的字段列表 |
| error_message | TEXT | 失败原因 |
| created_at | DATETIME | 爬取时间 |

### 6.2 监控指标

运营后台展示：

| 指标 | 来源 |
|------|------|
| 待爬取数量 | candidate 状态 VTuber 计数 |
| 今日成功/失败 | crawl_log 当日统计 |
| 最近失败列表 | crawl_log 中 status=failed 的记录 |

---

## 七、反爬与限速

| 措施 | 说明 |
|------|------|
| 请求间隔 | 每次请求后休眠 2-5 秒（随机） |
| User-Agent | 设置正常浏览器 UA |
| 超时设置 | 连接 10 秒，读取 30 秒 |
| 失败重试 | 单个 VTuber 最多重试 3 次，仍失败则跳过 |

**量级估算**：即使 1000 个 candidate，每个 3 秒间隔，全部跑完约 50 分钟，凌晨执行完全可接受。

---

## 八、依赖引入

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
</dependency>
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

---

*文档版本：v2.0 | 更新日期：2026-08-18*
*变更说明：从独立 Python 服务简化为 Spring Boot 定时任务*
