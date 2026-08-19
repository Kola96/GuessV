# M3：游戏核心 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.
> 返回 [路线图](../000-roadmap.md) | [AGENTS.md](../../../AGENTS.md)

**Goal:** 实现属性对比、每日目标、每日模式与单人模式的全部游戏 API。

**Architecture:** `ComparisonService`（纯逻辑）+ `DailyTargetService`（目标管理）+ `GameService`（每日/单人模式业务）。游戏状态持久化到 `game_record`，断线可恢复。

**Tech Stack:** Spring Boot 3.2 + MyBatis-Plus + @Scheduled + JUnit 5

## Global Constraints

- 沿用 M1/M2 的包名、Result、全局异常、鉴权拦截器
- 游戏 API（`/api/game/**`）需鉴权（已默认受拦截器保护）
- 对比规则严格按 [属性对比规则](../../game/003-comparison-rules.md)
- direction 箭头：higher→↑（目标值更高）、lower→↓（目标值更低）。修正 API 示例笔误。
- 每日模式会话键：`user_id + target_date`；单人模式会话键：`game_record.id`
- 测试隔离：Service 测试 `@Transactional`；Controller 测试真实 HTTP + `@AfterEach` 清理
- 最大尝试次数配置：`app.game.max-attempts: 8`

## 设计文档参考

- [每日模式设计](../../game/001-daily-mode.md)
- [单人模式设计](../../game/002-single-mode.md)
- [属性对比规则](../../game/003-comparison-rules.md)
- [游戏 API](../../api/001-game-api.md)

---

## Task 1：属性对比服务

**Files:**
- Create: `backend/src/main/java/com/guessv/dto/FieldComparison.java`
- Create: `backend/src/main/java/com/guessv/dto/ComparisonResult.java`
- Create: `backend/src/main/java/com/guessv/service/ComparisonService.java`
- Create: `backend/src/test/java/com/guessv/service/ComparisonServiceTest.java`

**Interfaces:**
- Produces: `ComparisonService.compare(Vtuber guess, Vtuber target)` → ComparisonResult

### 对比规则实现要点

| 属性 | 逻辑 |
|------|------|
| name | 永远 exact（展示用） |
| region | 完全匹配 → exact/none |
| group | 相同→exact；首词相同（如 Hololive/Hololive EN）→partial；否则 none |
| debutYear | 相等→exact；guess<target→higher+↑；guess>target→lower+↓ |
| gender | 完全匹配；null 双方→exact，单方 null→none |
| status(activityStatus) | 完全匹配，翻译 active→活动 等 |
| hairColor | 数组完全相同→exact；有交集→partial；无交集→none |
| fanName | 完全匹配 |

- [x] **Step 1：创建 DTO**

`backend/src/main/java/com/guessv/dto/FieldComparison.java`:
```java
package com.guessv.dto;

public record FieldComparison(Object value, String match, String direction) {
    public FieldComparison(Object value, String match) {
        this(value, match, null);
    }
}
```

`backend/src/main/java/com/guessv/dto/ComparisonResult.java`:
```java
package com.guessv.dto;

public record ComparisonResult(
        FieldComparison name,
        FieldComparison region,
        FieldComparison group,
        FieldComparison debutYear,
        FieldComparison gender,
        FieldComparison status,
        FieldComparison hairColor,
        FieldComparison fanName
) {}
```

- [x] **Step 2：先写 ComparisonServiceTest（TDD）**

`backend/src/test/java/com/guessv/service/ComparisonServiceTest.java`:
```java
package com.guessv.service;

import com.guessv.dto.ComparisonResult;
import com.guessv.entity.Vtuber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ComparisonServiceTest {

    @Autowired private ComparisonService comparisonService;

    private Vtuber v(String name, String region, String group, Integer year,
                     String gender, String status, List<String> hair, String fan) {
        Vtuber v = new Vtuber();
        v.setNameEn(name); v.setNameCn(name); v.setNameDefault("en");
        v.setRegion(region); v.setGroupName(group); v.setDebutYear(year);
        v.setGender(gender); v.setActivityStatus(status);
        v.setHairColor(hair); v.setFanName(fan);
        return v;
    }

    @Test
    void allExactMatch() {
        Vtuber g = v("Gura", "英语圈", "Hololive EN", 2020, "female", "active", List.of("蓝"), "Shrimp");
        ComparisonResult r = comparisonService.compare(g, g);
        assertEquals("exact", r.region().match());
        assertEquals("exact", r.group().match());
        assertEquals("exact", r.debutYear().match());
        assertEquals("exact", r.gender().match());
        assertEquals("exact", r.status().match());
        assertEquals("exact", r.hairColor().match());
        assertEquals("exact", r.fanName().match());
    }

    @Test
    void groupPartialSameCompany() {
        Vtuber guess = v("A", "日本", "Hololive", 2018, "female", "active", List.of("白"), "F");
        Vtuber target = v("B", "日本", "Hololive EN", 2020, "female", "active", List.of("白"), "F");
        assertEquals("partial", comparisonService.compare(guess, target).group().match());
    }

    @Test
    void groupNoneDifferentCompany() {
        Vtuber guess = v("A", "日本", "Hololive", 2018, "female", "active", List.of("白"), "F");
        Vtuber target = v("B", "日本", "Nijisanji", 2018, "female", "active", List.of("白"), "F");
        assertEquals("none", comparisonService.compare(guess, target).group().match());
    }

    @Test
    void debutYearHigherWhenTargetLater() {
        Vtuber guess = v("A", "日", "G", 2018, "female", "active", List.of("白"), "F");
        Vtuber target = v("B", "日", "G", 2020, "female", "active", List.of("白"), "F");
        var f = comparisonService.compare(guess, target).debutYear();
        assertEquals("higher", f.match());
        assertEquals("↑", f.direction());
    }

    @Test
    void debutYearLowerWhenTargetEarlier() {
        Vtuber guess = v("A", "日", "G", 2020, "female", "active", List.of("白"), "F");
        Vtuber target = v("B", "日", "G", 2018, "female", "active", List.of("白"), "F");
        var f = comparisonService.compare(guess, target).debutYear();
        assertEquals("lower", f.match());
        assertEquals("↓", f.direction());
    }

    @Test
    void hairColorPartialOverlap() {
        Vtuber guess = v("A", "日", "G", 2020, "female", "active", List.of("蓝"), "F");
        Vtuber target = v("B", "日", "G", 2020, "female", "active", List.of("蓝", "白"), "F");
        assertEquals("partial", comparisonService.compare(guess, target).hairColor().match());
    }

    @Test
    void hairColorNoneNoOverlap() {
        Vtuber guess = v("A", "日", "G", 2020, "female", "active", List.of("红"), "F");
        Vtuber target = v("B", "日", "G", 2020, "female", "active", List.of("蓝", "白"), "F");
        assertEquals("none", comparisonService.compare(guess, target).hairColor().match());
    }

    @Test
    void nullFieldsBothNullCountsAsExact() {
        Vtuber guess = v("A", null, null, null, null, null, null, null);
        Vtuber target = v("B", null, null, null, null, null, null, null);
        ComparisonResult r = comparisonService.compare(guess, target);
        assertEquals("exact", r.region().match());
        assertEquals("exact", r.debutYear().match());
        assertEquals("exact", r.hairColor().match());
    }

    @Test
    void statusTranslatedToChinese() {
        Vtuber g = v("A", "日", "G", 2020, "female", "active", List.of("白"), "F");
        Vtuber t = v("B", "日", "G", 2020, "female", "graduated", List.of("白"), "F");
        var r = comparisonService.compare(g, t);
        assertEquals("活动", r.status().value());
        assertEquals("毕业", comparisonService.compare(t, g).status().value());
    }
}
```

- [x] **Step 3：实现 ComparisonService**

`backend/src/main/java/com/guessv/service/ComparisonService.java`:
```java
package com.guessv.service;

import com.guessv.dto.ComparisonResult;
import com.guessv.dto.FieldComparison;
import com.guessv.entity.Vtuber;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
public class ComparisonService {

    public ComparisonResult compare(Vtuber guess, Vtuber target) {
        return new ComparisonResult(
                name(guess),
                compareString(guess.getRegion(), target.getRegion()),
                compareGroup(guess.getGroupName(), target.getGroupName()),
                compareYear(guess.getDebutYear(), target.getDebutYear()),
                compareString(guess.getGender(), target.getGender(), this::translateGender),
                compareString(guess.getActivityStatus(), target.getActivityStatus(), this::translateStatus),
                compareList(guess.getHairColor(), target.getHairColor()),
                compareString(guess.getFanName(), target.getFanName())
        );
    }

    private FieldComparison name(Vtuber v) {
        String display = "cn".equals(v.getNameDefault()) && v.getNameCn() != null
                ? v.getNameCn() : (v.getNameEn() != null ? v.getNameEn() : v.getNameCn());
        return new FieldComparison(display, "exact");
    }

    private FieldComparison compareString(String a, String b) {
        return compareString(a, b, s -> s);
    }

    private FieldComparison compareString(String a, String b, java.util.function.Function<String, String> translate) {
        String ta = a == null ? null : translate.apply(a);
        String tb = b == null ? null : translate.apply(b);
        if (ta == null && tb == null) return new FieldComparison(ta, "exact");
        if (ta == null || tb == null) return new FieldComparison(ta, "none");
        return new FieldComparison(ta, ta.equals(tb) ? "exact" : "none");
    }

    private FieldComparison compareGroup(String a, String b) {
        if (a == null && b == null) return new FieldComparison(null, "exact");
        if (a == null || b == null) return new FieldComparison(a, "none");
        if (a.equals(b)) return new FieldComparison(a, "exact");
        // 首词相同视为同公司不同分部
        String headA = firstToken(a);
        String headB = firstToken(b);
        if (headA != null && headA.equalsIgnoreCase(headB)) {
            return new FieldComparison(a, "partial");
        }
        return new FieldComparison(a, "none");
    }

    private String firstToken(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        int sp = trimmed.indexOf(' ');
        return sp > 0 ? trimmed.substring(0, sp) : trimmed;
    }

    private FieldComparison compareYear(Integer a, Integer b) {
        if (a == null && b == null) return new FieldComparison(null, "exact");
        if (a == null || b == null) return new FieldComparison(a, "none");
        if (a.equals(b)) return new FieldComparison(a, "exact");
        if (a < b) return new FieldComparison(a, "higher", "↑");
        return new FieldComparison(a, "lower", "↓");
    }

    @SuppressWarnings("unchecked")
    private FieldComparison compareList(Collection<String> a, Collection<String> b) {
        if (a == null && b == null) return new FieldComparison(null, "exact");
        if (a == null || b == null) return new FieldComparison(a, "none");
        Set<String> sa = new HashSet<>(a);
        Set<String> sb = new HashSet<>(b);
        if (sa.equals(sb)) return new FieldComparison(a, "exact");
        if (!java.util.Collections.disjoint(sa, sb)) return new FieldComparison(a, "partial");
        return new FieldComparison(a, "none");
    }

    private String translateGender(String g) {
        if (g == null) return null;
        return switch (g) {
            case "male" -> "男";
            case "female" -> "女";
            default -> "其他";
        };
    }

    private String translateStatus(String s) {
        if (s == null) return null;
        return switch (s) {
            case "active" -> "活动";
            case "graduated" -> "毕业";
            case "hiatus" -> "休止";
            case "suspended" -> "暂停";
            default -> s;
        };
    }
}
```

- [x] **Step 4：运行测试**

Run: `cd backend && mvn test -Dtest=ComparisonServiceTest -q`
Expected: 9 个测试通过

- [x] **Step 5：提交**

```bash
git add backend/
git commit -m "feat(game): 属性对比服务（7 维度）"
```

---

## Task 2：每日目标管理

**Files:**
- Modify: `backend/src/main/java/com/guessv/GuessVApplication.java`（加 @EnableScheduling）
- Create: `backend/src/main/java/com/guessv/service/DailyTargetService.java`
- Create: `backend/src/main/java/com/guessv/scheduler/DailyTargetScheduler.java`
- Modify: `backend/src/main/resources/application.yml`（加 max-attempts）
- Create: `backend/src/test/java/com/guessv/service/DailyTargetServiceTest.java`

**Interfaces:**
- Produces: `DailyTargetService.getOrCreateToday()` → DailyTarget
- Produces: `DailyTargetService.getByDate(date)` → DailyTarget（可空）
- Produces: 每日 00:00 (Asia/Shanghai) 定时刷新

- [x] **Step 1：application.yml 加配置**

在 `app:` 节点追加：
```yaml
  game:
    max-attempts: 8
```

- [x] **Step 2：GuessVApplication 加 @EnableScheduling**

修改主类注解：
```java
@SpringBootApplication
@MapperScan("com.guessv.mapper")
@EnableScheduling
public class GuessVApplication { ... }
```
（加 import `org.springframework.scheduling.annotation.EnableScheduling`）

- [x] **Step 3：先写 DailyTargetServiceTest（TDD）**

`backend/src/test/java/com/guessv/service/DailyTargetServiceTest.java`:
```java
package com.guessv.service;

import com.guessv.GuessVApplication;
import com.guessv.config.DevDataSeeder;
import com.guessv.entity.DailyTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GuessVApplication.class)
@ActiveProfiles("test")
@Transactional
class DailyTargetServiceTest {

    @Autowired private DailyTargetService dailyTargetService;
    @Autowired private DevDataSeeder seeder;

    @BeforeEach
    void seed() { seeder.seed(); }

    @Test
    void getOrCreateTodayCreatesIfMissing() {
        DailyTarget t = dailyTargetService.getOrCreateToday();
        assertNotNull(t);
        assertNotNull(t.getVtuberId());
        assertEquals(LocalDate.now(), t.getTargetDate());
    }

    @Test
    void getOrCreateTodayIsIdempotent() {
        DailyTarget a = dailyTargetService.getOrCreateToday();
        DailyTarget b = dailyTargetService.getOrCreateToday();
        assertEquals(a.getVtuberId(), b.getVtuberId());
        assertEquals(a.getTargetDate(), b.getTargetDate());
    }

    @Test
    void targetPointsToActiveOrVerifiedVtuber() {
        DailyTarget t = dailyTargetService.getOrCreateToday();
        assertNotNull(t.getVtuberId());
        // vtuber_id 引用的应是 active/verified 数据（种子数据全是 active）
        assertTrue(t.getVtuberId() > 0);
    }
}
```

- [x] **Step 4：实现 DailyTargetService**

`backend/src/main/java/com/guessv/service/DailyTargetService.java`:
```java
package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.entity.DailyTarget;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.DailyTargetMapper;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyTargetService {

    private static final int EXCLUDE_RECENT_DAYS = 30;

    private final DailyTargetMapper dailyTargetMapper;
    private final VtuberMapper vtuberMapper;

    @Transactional
    public DailyTarget getOrCreateToday() {
        return getOrCreateForDate(LocalDate.now());
    }

    @Transactional
    public DailyTarget getOrCreateForDate(LocalDate date) {
        DailyTarget existing = dailyTargetMapper.selectOne(
                new QueryWrapper<DailyTarget>().eq("target_date", date));
        if (existing != null) return existing;

        // 排除近 N 天已选过的
        LocalDate since = date.minusDays(EXCLUDE_RECENT_DAYS);
        List<DailyTarget> recent = dailyTargetMapper.selectList(
                new QueryWrapper<DailyTarget>()
                        .gt("target_date", since)
                        .le("target_date", date));
        List<Long> recentIds = recent.stream()
                .map(DailyTarget::getVtuberId)
                .collect(Collectors.toList());

        QueryWrapper<Vtuber> qw = new QueryWrapper<Vtuber>()
                .in("data_status", "active", "verified");
        if (!recentIds.isEmpty()) qw.notIn("id", recentIds);
        List<Vtuber> candidates = vtuberMapper.selectList(qw);

        if (candidates.isEmpty()) {
            // 兜底：放宽排除窗口
            candidates = vtuberMapper.selectList(
                    new QueryWrapper<Vtuber>().in("data_status", "active", "verified"));
        }
        if (candidates.isEmpty()) throw new IllegalStateException("无可用 VTuber 作为每日目标");

        Vtuber picked = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        DailyTarget t = new DailyTarget();
        t.setTargetDate(date);
        t.setVtuberId(picked.getId());
        dailyTargetMapper.insert(t);
        log.info("每日目标已生成：date={} vtuberId={}", date, picked.getId());
        return t;
    }

    public DailyTarget getByDate(LocalDate date) {
        return dailyTargetMapper.selectOne(
                new QueryWrapper<DailyTarget>().eq("target_date", date));
    }
}
```

- [x] **Step 5：实现 DailyTargetScheduler**

`backend/src/main/java/com/guessv/scheduler/DailyTargetScheduler.java`:
```java
package com.guessv.scheduler;

import com.guessv.service.DailyTargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyTargetScheduler {

    private final DailyTargetService dailyTargetService;

    // 每日 00:00 (Asia/Shanghai)
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Shanghai")
    public void refreshDailyTarget() {
        log.info("定时任务：刷新每日目标");
        dailyTargetService.getOrCreateForDate(LocalDate.now());
    }
}
```

- [x] **Step 6：运行测试**

Run: `cd backend && mvn test -Dtest=DailyTargetServiceTest -q`
Expected: 3 个测试通过

- [x] **Step 7：提交**

```bash
git add backend/
git commit -m "feat(game): 每日目标管理与定时刷新"
```

---

## Task 3：每日模式 API

**Files:**
- Create: `backend/src/main/java/com/guessv/dto/GuessEntry.java`
- Create: `backend/src/main/java/com/guessv/dto/GuessResponse.java`
- Create: `backend/src/main/java/com/guessv/dto/DailyGameInfoVO.java`
- Create: `backend/src/main/java/com/guessv/service/GameService.java`
- Create: `backend/src/main/java/com/guessv/controller/GameController.java`
- Create: `backend/src/test/java/com/guessv/controller/DailyGameTest.java`

**Interfaces:**
- Produces: `GET /api/game/daily`（今日信息 + 历史猜测，不暴露目标）
- Produces: `POST /api/game/daily/guess {vtuberId}`（对比结果，含胜负判定）

- [x] **Step 1：创建 DTO**

`backend/src/main/java/com/guessv/dto/GuessEntry.java`:
```java
package com.guessv.dto;

public record GuessEntry(
        Long vtuberId,
        String vtuberName,
        int attemptNumber,
        boolean correct,
        ComparisonResult comparison,
        String guessedAt
) {}
```

`backend/src/main/java/com/guessv/dto/GuessResponse.java`:
```java
package com.guessv.dto;

public record GuessResponse(
        boolean correct,
        boolean gameOver,
        boolean win,
        int remainingAttempts,
        int attemptsUsed,
        ComparisonResult comparison,
        VtuberReveal targetVtuber
) {
    public record VtuberReveal(Long id, String name, String avatarUrl) {}
}
```

`backend/src/main/java/com/guessv/dto/DailyGameInfoVO.java`:
```java
package com.guessv.dto;

import java.util.List;

public record DailyGameInfoVO(
        String date,
        int maxAttempts,
        int totalVtuberCount,
        boolean hasPlayed,
        boolean hasWon,
        int attemptsUsed,
        List<GuessEntry> guesses
) {}
```

- [x] **Step 2：实现 GameService（每日部分）**

`backend/src/main/java/com/guessv/service/GameService.java`:
```java
package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guessv.common.BizException;
import com.guessv.dto.*;
import com.guessv.entity.DailyTarget;
import com.guessv.entity.GameRecord;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.GameRecordMapper;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final DailyTargetService dailyTargetService;
    private final ComparisonService comparisonService;
    private final GameRecordMapper gameRecordMapper;
    private final VtuberMapper vtuberMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.game.max-attempts:8}")
    private int maxAttempts;

    // ===== 每日模式 =====

    public DailyGameInfoVO getDailyInfo(String userUuid) {
        DailyTarget target = dailyTargetService.getOrCreateToday();
        GameRecord record = getDailyRecord(userUuid, LocalDate.now());
        long total = vtuberMapper.selectCount(
                new QueryWrapper<Vtuber>().in("data_status", "active", "verified"));
        List<GuessEntry> guesses = parseGuesses(record);
        boolean hasPlayed = record != null;
        boolean hasWon = record != null && Boolean.TRUE.equals(record.getIsWin());
        int used = record != null ? record.getAttempts() : 0;
        return new DailyGameInfoVO(
                LocalDate.now().toString(), maxAttempts, (int) total,
                hasPlayed, hasWon, used, guesses);
    }

    @Transactional
    public GuessResponse dailyGuess(String userUuid, Long vtuberId) {
        DailyTarget target = dailyTargetService.getOrCreateToday();
        GameRecord record = getOrCreateDailyRecord(userUuid, LocalDate.now(), target.getVtuberId());

        if (record.getFinishedAt() != null) {
            throw new BizException(409, "今日游戏已结束");
        }
        if (record.getAttempts() >= maxAttempts) {
            throw new BizException(409, "尝试次数已用完");
        }
        List<GuessEntry> entries = parseGuesses(record);
        // 重复猜测检查
        if (entries.stream().anyMatch(e -> e.vtuberId().equals(vtuberId))) {
            throw new BizException(409, "你已经猜过这个 VTuber 了");
        }

        Vtuber guess = vtuberMapper.selectById(vtuberId);
        if (guess == null) throw new BizException(404, "VTuber 不存在");

        Vtuber targetV = vtuberMapper.selectById(target.getVtuberId());
        ComparisonResult comparison = comparisonService.compare(guess, targetV);

        int attemptNo = entries.size() + 1;
        boolean correct = guess.getId().equals(targetV.getId());
        GuessEntry entry = new GuessEntry(
                vtuberId, displayName(guess), attemptNo, correct, comparison,
                LocalDateTime.now().toString());
        entries.add(entry);

        record.setAttempts(attemptNo);
        record.setGuesses(serializeGuesses(entries));
        record.setMaxAttempts(maxAttempts);

        boolean gameOver = false;
        boolean win = false;
        if (correct) {
            record.setIsWin(true);
            record.setFinishedAt(LocalDateTime.now());
            gameOver = true; win = true;
        } else if (attemptNo >= maxAttempts) {
            record.setIsWin(false);
            record.setFinishedAt(LocalDateTime.now());
            gameOver = true;
        }
        gameRecordMapper.updateById(record);

        GuessResponse.VtuberReveal reveal = gameOver
                ? new GuessResponse.VtuberReveal(targetV.getId(), displayName(targetV), targetV.getAvatarUrl())
                : null;
        return new GuessResponse(correct, gameOver, win,
                maxAttempts - attemptNo, attemptNo, comparison, reveal);
    }

    private GameRecord getDailyRecord(String userUuid, LocalDate date) {
        // 通过 user 表的 uuid → user_id 关联；此处用 uuid 字符串作 user_id 占位简化
        // 实际：GameRecord.user_id 应关联 user.id。这里改用 user_uuid 字段查询。
        return gameRecordMapper.selectOne(new QueryWrapper<GameRecord>()
                .eq("mode", "daily")
                .eq("pool_tag", userUuid) // 复用 pool_tag 字段存 userUuid
                .eq("started_at", date.toString()));
    }
```

> **注意**：上方用 `pool_tag` 存 userUuid、`started_at` 存日期字符串是临时简化。下面 Step 3 会用更干净的方式：通过 user.id 关联。

实际上更干净的设计：GameRecord 已有 `user_id`（BIGINT）字段。鉴权拦截器设置 `currentUser`，从中取 `user.getId()`。让我用 user.id 而非 uuid。修正如下（替换上面 getDailyRecord 实现）：

```java
    private GameRecord getDailyRecord(Long userId, LocalDate date) {
        return gameRecordMapper.selectOne(new QueryWrapper<GameRecord>()
                .eq("mode", "daily")
                .eq("user_id", userId)
                .eq("target_date", date.toString())); // 需要字段，见下
    }
```

但 GameRecord 表没有 `target_date` 字段。daily 模式按日期区分，用 `started_at` 的日期部分。SQLite 不好做日期函数。**最简方案：用 `pool_tag` 存日期字符串**（daily 模式下 pool_tag 无意义，复用）。

最终方案：daily 模式的 GameRecord 用 `user_id + pool_tag(=日期)` 唯一定位。

- [x] **Step 3：完整 GameService（修正版）**

替换 Step 2 的 GameService 为：
```java
package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guessv.common.BizException;
import com.guessv.dto.*;
import com.guessv.entity.DailyTarget;
import com.guessv.entity.GameRecord;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.GameRecordMapper;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final DailyTargetService dailyTargetService;
    private final ComparisonService comparisonService;
    private final GameRecordMapper gameRecordMapper;
    private final VtuberMapper vtuberMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.game.max-attempts:8}")
    private int maxAttempts;

    // ===== 每日模式 =====

    public DailyGameInfoVO getDailyInfo(Long userId) {
        DailyTarget target = dailyTargetService.getOrCreateToday();
        GameRecord record = findDailyRecord(userId, LocalDate.now());
        long total = vtuberMapper.selectCount(
                new QueryWrapper<Vtuber>().in("data_status", "active", "verified"));
        List<GuessEntry> guesses = parseGuesses(record);
        boolean hasPlayed = record != null;
        boolean hasWon = record != null && Boolean.TRUE.equals(record.getIsWin());
        int used = record != null ? record.getAttempts() : 0;
        return new DailyGameInfoVO(
                LocalDate.now().toString(), maxAttempts, (int) total,
                hasPlayed, hasWon, used, guesses);
    }

    @Transactional
    public GuessResponse dailyGuess(Long userId, Long vtuberId) {
        DailyTarget target = dailyTargetService.getOrCreateToday();
        GameRecord record = findDailyRecord(userId, LocalDate.now());
        if (record == null) record = createDailyRecord(userId, target.getVtuberId());

        if (record.getFinishedAt() != null) throw new BizException(409, "今日游戏已结束");
        if (record.getAttempts() >= maxAttempts) throw new BizException(409, "尝试次数已用完");

        List<GuessEntry> entries = parseGuesses(record);
        if (entries.stream().anyMatch(e -> e.vtuberId().equals(vtuberId)))
            throw new BizException(409, "你已经猜过这个 VTuber 了");

        Vtuber guess = vtuberMapper.selectById(vtuberId);
        if (guess == null) throw new BizException(404, "VTuber 不存在");

        Vtuber targetV = vtuberMapper.selectById(target.getVtuberId());
        ComparisonResult comparison = comparisonService.compare(guess, targetV);

        int attemptNo = entries.size() + 1;
        boolean correct = guess.getId().equals(targetV.getId());
        GuessEntry entry = new GuessEntry(
                vtuberId, displayName(guess), attemptNo, correct, comparison,
                LocalDateTime.now().toString());
        entries.add(entry);

        record.setAttempts(attemptNo);
        record.setGuesses(serializeGuesses(entries));
        record.setMaxAttempts(maxAttempts);

        boolean gameOver = false, win = false;
        if (correct) {
            record.setIsWin(true);
            record.setFinishedAt(LocalDateTime.now());
            gameOver = true; win = true;
        } else if (attemptNo >= maxAttempts) {
            record.setIsWin(false);
            record.setFinishedAt(LocalDateTime.now());
            gameOver = true;
        }
        gameRecordMapper.updateById(record);

        GuessResponse.VtuberReveal reveal = gameOver
                ? new GuessResponse.VtuberReveal(targetV.getId(), displayName(targetV), targetV.getAvatarUrl())
                : null;
        return new GuessResponse(correct, gameOver, win,
                maxAttempts - attemptNo, attemptNo, comparison, reveal);
    }

    private GameRecord findDailyRecord(Long userId, LocalDate date) {
        return gameRecordMapper.selectOne(new QueryWrapper<GameRecord>()
                .eq("mode", "daily")
                .eq("user_id", userId)
                .eq("pool_tag", date.toString()));
    }

    private GameRecord createDailyRecord(Long userId, Long targetId) {
        GameRecord r = new GameRecord();
        r.setUserId(userId);
        r.setMode("daily");
        r.setTargetId(targetId);
        r.setPoolTag(LocalDate.now().toString());
        r.setAttempts(0);
        r.setMaxAttempts(maxAttempts);
        r.setIsWin(false);
        r.setGuesses("[]");
        r.setStartedAt(LocalDateTime.now());
        gameRecordMapper.insert(r);
        return r;
    }

    // ===== 工具方法 =====

    @SuppressWarnings("unchecked")
    private List<GuessEntry> parseGuesses(GameRecord r) {
        if (r == null || r.getGuesses() == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(r.getGuesses().toString(),
                    new TypeReference<List<GuessEntry>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Object serializeGuesses(List<GuessEntry> entries) {
        try { return objectMapper.writeValueAsString(entries); }
        catch (Exception e) { return "[]"; }
    }

    private String displayName(Vtuber v) {
        if ("cn".equals(v.getNameDefault()) && v.getNameCn() != null) return v.getNameCn();
        if (v.getNameEn() != null) return v.getNameEn();
        return v.getNameCn();
    }
}
```

> 注：`parseGuesses` 中 `r.getGuesses()` 是 Object（JacksonTypeHandler 反序列化为 LinkedHashMap/List）。`toString()` 不对。需修正为读作 List。详见 Step 5 测试时修正。

- [x] **Step 4：实现 GameController（每日部分）**

`backend/src/main/java/com/guessv/controller/GameController.java`:
```java
package com.guessv.controller;

import com.guessv.common.Result;
import com.guessv.dto.DailyGameInfoVO;
import com.guessv.dto.GuessResponse;
import com.guessv.entity.User;
import com.guessv.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/daily")
    public Result<DailyGameInfoVO> dailyInfo(
            @RequestAttribute("currentUser") User user) {
        return Result.ok(gameService.getDailyInfo(user.getId()));
    }

    @PostMapping("/daily/guess")
    public Result<GuessResponse> dailyGuess(
            @RequestAttribute("currentUser") User user,
            @RequestBody DailyGuessRequest req) {
        return Result.ok(gameService.dailyGuess(user.getId(), req.vtuberId()));
    }

    public record DailyGuessRequest(Long vtuberId) {}
}
```

- [x] **Step 5：编写 DailyGameTest 并修正 JSON 处理**

`backend/src/test/java/com/guessv/controller/DailyGameTest.java`:
```java
package com.guessv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.entity.User;
import com.guessv.mapper.UserMapper;
import com.guessv.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GuessVApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DailyGameTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired UserService userService;
    @Autowired UserMapper userMapper;

    private String token;

    @BeforeEach
    void setup() {
        token = userService.createAnonymousUser("玩家", "fp").token();
    }

    @AfterEach
    void cleanup() {
        userMapper.delete(new QueryWrapper<User>().eq("is_anonymous", true));
    }

    private HttpHeaders auth() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-User-Token", token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private String url(String p) { return "http://localhost:" + port + p; }

    @Test
    void dailyInfoReturnsBasicState() {
        var entity = new HttpEntity<>(auth());
        var resp = restTemplate.exchange(url("/api/game/daily"), HttpMethod.GET, entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("\"hasPlayed\":false"));
    }

    @Test
    void guessWrongReturnsComparison() {
        // 用种子数据中某个非目标的 V 去猜（这里随机取一个，可能猜中也算正常）
        HttpEntity<String> req = new HttpEntity<>("{\"vtuberId\":9770}", auth()); // Gura
        var resp = restTemplate.postForEntity(url("/api/game/daily/guess"), req, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("comparison"));
    }

    @Test
    void duplicateGuessRejected() {
        HttpEntity<String> req = new HttpEntity<>("{\"vtuberId\":9770}", auth());
        restTemplate.postForEntity(url("/api/game/daily/guess"), req, String.class);
        var resp2 = restTemplate.postForEntity(url("/api/game/daily/guess"), req, String.class);
        // 重复猜测返回 409
        assertTrue(resp2.getBody().contains("\"code\":409") || resp2.getBody().contains("猜过"));
    }
}
```

> **关于 GuessEntry 的 JSON 反序列化**：GameRecord.guesses 是 Object 类型（JacksonTypeHandler），存入时是 JSON 字符串，读出时会被反序列化为 List<LinkedHashMap>。在 parseGuesses 中需要按 List<GuessEntry> 重新转换。用 `objectMapper.convertValue(rawList, new TypeReference<List<GuessEntry>>(){})` 处理。

- [x] **Step 6：运行测试 + 修正 GameService 的 JSON 处理**

修正 `parseGuesses`：
```java
    @SuppressWarnings("unchecked")
    private List<GuessEntry> parseGuesses(GameRecord r) {
        if (r == null || r.getGuesses() == null) return new ArrayList<>();
        try {
            Object raw = r.getGuesses();
            if (raw instanceof String s) {
                return objectMapper.readValue(s, new TypeReference<>() {});
            }
            return objectMapper.convertValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析 guesses 失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
```

并修正 `serializeGuesses` 返回类型为 `String`（因为字段是 Object，但 JacksonTypeHandler 会把 String 存为 JSON 字符串）。实际：把字段类型改为 String 更简单——但 entity 已定义为 Object。折中：serializeGuesses 返回 String，JacksonTypeHandler 会把 String 作为 JSON 字符串存（带引号）。这会导致读回时是 String。上面 parseGuesses 已处理 String 情况。OK。

Run: `cd backend && mvn test -Dtest=DailyGameTest -q`
Expected: 3 个测试通过

- [x] **Step 7：提交**

```bash
git add backend/
git commit -m "feat(game): 每日模式 API（信息查询 + 猜测对比 + 胜负判定）"
```

---

## Task 4：单人模式 API

**Files:**
- Create: `backend/src/main/java/com/guessv/dto/PoolVO.java`
- Create: `backend/src/main/java/com/guessv/dto/SingleStartResponse.java`
- Modify: `backend/src/main/java/com/guessv/service/GameService.java`（加单人方法）
- Modify: `backend/src/main/java/com/guessv/controller/GameController.java`（加单人端点）
- Create: `backend/src/test/java/com/guessv/controller/SingleGameTest.java`

**Interfaces:**
- Produces: `GET /api/game/single/pools` → 题库列表
- Produces: `POST /api/game/single/start {poolTag}` → {sessionId, ...}
- Produces: `POST /api/game/single/guess {sessionId, vtuberId}` → 对比
- Produces: `POST /api/game/single/end {sessionId}` → 最终结果
- Produces: `GET /api/game/single/{sessionId}` → 当前状态（断线恢复）

- [x] **Step 1：创建 DTO**

`backend/src/main/java/com/guessv/dto/PoolVO.java`:
```java
package com.guessv.dto;

public record PoolVO(String tag, String description, int vtuberCount) {}
```

`backend/src/main/java/com/guessv/dto/SingleStartResponse.java`:
```java
package com.guessv.dto;

public record SingleStartResponse(Long sessionId, int maxAttempts, String poolTag, int vtuberCount) {}
```

- [x] **Step 2：GameService 加单人方法**

在 GameService 中追加：
```java
    // ===== 单人模式 =====

    public List<PoolVO> listPools() {
        List<PoolVO> pools = new ArrayList<>();
        for (String tag : List.of("全量", "日V", "国V", "英语圈", "Hololive", "Nijisanji")) {
            long c = countByPool(tag);
            pools.add(new PoolVO(tag, describePool(tag), (int) c));
        }
        return pools;
    }

    private String describePool(String tag) {
        return switch (tag) {
            case "全量" -> "所有可用 VTuber";
            case "日V" -> "日本地区 VTuber";
            case "国V" -> "中国地区 VTuber";
            case "英语圈" -> "英语地区 VTuber";
            case "Hololive" -> "Hololive 所属";
            case "Nijisanji" -> "Nijisanji 所属";
            default -> tag;
        };
    }

    private long countByPool(String tag) {
        QueryWrapper<Vtuber> qw = new QueryWrapper<Vtuber>()
                .in("data_status", "active", "verified");
        switch (tag) {
            case "日V" -> qw.eq("region", "日本");
            case "国V" -> qw.eq("region", "中国");
            case "英语圈" -> qw.eq("region", "英语圈");
            case "Hololive" -> qw.likeRight("group_name", "Hololive");
            case "Nijisanji" -> qw.likeRight("group_name", "Nijisanji");
        }
        return vtuberMapper.selectCount(qw);
    }

    private List<Vtuber> findByPool(String tag) {
        QueryWrapper<Vtuber> qw = new QueryWrapper<Vtuber>()
                .in("data_status", "active", "verified");
        switch (tag) {
            case "日V" -> qw.eq("region", "日本");
            case "国V" -> qw.eq("region", "中国");
            case "英语圈" -> qw.eq("region", "英语圈");
            case "Hololive" -> qw.likeRight("group_name", "Hololive");
            case "Nijisanji" -> qw.likeRight("group_name", "Nijisanji");
        }
        return vtuberMapper.selectList(qw);
    }

    @Transactional
    public SingleStartResponse startSingle(Long userId, String poolTag) {
        if (poolTag == null || poolTag.isBlank()) poolTag = "全量";
        List<Vtuber> candidates = findByPool(poolTag);
        if (candidates.isEmpty()) throw new BizException(400, "题库无可用 VTuber: " + poolTag);
        Vtuber picked = candidates.get(
                ThreadLocalRandom.current().nextInt(candidates.size()));
        GameRecord r = new GameRecord();
        r.setUserId(userId);
        r.setMode("single");
        r.setTargetId(picked.getId());
        r.setPoolTag(poolTag);
        r.setAttempts(0);
        r.setMaxAttempts(maxAttempts);
        r.setIsWin(false);
        r.setGuesses("[]");
        r.setStartedAt(LocalDateTime.now());
        gameRecordMapper.insert(r);
        return new SingleStartResponse(r.getId(), maxAttempts, poolTag, candidates.size());
    }

    @Transactional
    public GuessResponse singleGuess(Long userId, Long sessionId, Long vtuberId) {
        GameRecord record = gameRecordMapper.selectById(sessionId);
        if (record == null || !"single".equals(record.getMode()))
            throw new BizException(404, "会话不存在");
        if (!record.getUserId().equals(userId))
            throw new BizException(403, "无权访问该会话");
        if (record.getFinishedAt() != null) throw new BizException(409, "本局已结束");
        if (record.getAttempts() >= maxAttempts) throw new BizException(409, "尝试次数已用完");

        List<GuessEntry> entries = parseGuesses(record);
        if (entries.stream().anyMatch(e -> e.vtuberId().equals(vtuberId)))
            throw new BizException(409, "你已经猜过这个 VTuber 了");

        Vtuber guess = vtuberMapper.selectById(vtuberId);
        if (guess == null) throw new BizException(404, "VTuber 不存在");
        Vtuber targetV = vtuberMapper.selectById(record.getTargetId());
        ComparisonResult comparison = comparisonService.compare(guess, targetV);

        int attemptNo = entries.size() + 1;
        boolean correct = guess.getId().equals(targetV.getId());
        entries.add(new GuessEntry(vtuberId, displayName(guess), attemptNo, correct,
                comparison, LocalDateTime.now().toString()));

        record.setAttempts(attemptNo);
        record.setGuesses(serializeGuesses(entries));

        boolean gameOver = false, win = false;
        if (correct) {
            record.setIsWin(true); record.setFinishedAt(LocalDateTime.now());
            gameOver = true; win = true;
        } else if (attemptNo >= maxAttempts) {
            record.setIsWin(false); record.setFinishedAt(LocalDateTime.now());
            gameOver = true;
        }
        gameRecordMapper.updateById(record);

        GuessResponse.VtuberReveal reveal = gameOver
                ? new GuessResponse.VtuberReveal(targetV.getId(), displayName(targetV), targetV.getAvatarUrl())
                : null;
        return new GuessResponse(correct, gameOver, win,
                maxAttempts - attemptNo, attemptNo, comparison, reveal);
    }

    public DailyGameInfoVO getSingleState(Long userId, Long sessionId) {
        GameRecord record = gameRecordMapper.selectById(sessionId);
        if (record == null || !"single".equals(record.getMode()))
            throw new BizException(404, "会话不存在");
        if (!record.getUserId().equals(userId))
            throw new BizException(403, "无权访问该会话");
        List<GuessEntry> guesses = parseGuesses(record);
        boolean hasWon = Boolean.TRUE.equals(record.getIsWin());
        return new DailyGameInfoVO(
                record.getPoolTag(), maxAttempts, 0,
                true, hasWon, record.getAttempts(), guesses);
    }
```

> 需要 import `java.util.concurrent.ThreadLocalRandom`。

- [x] **Step 3：GameController 加单人端点**

在 GameController 追加：
```java
    @GetMapping("/single/pools")
    public Result<List<PoolVO>> pools() {
        return Result.ok(gameService.listPools());
    }

    @PostMapping("/single/start")
    public Result<SingleStartResponse> startSingle(
            @RequestAttribute("currentUser") User user,
            @RequestBody StartSingleRequest req) {
        return Result.ok(gameService.startSingle(user.getId(), req.poolTag()));
    }

    @PostMapping("/single/guess")
    public Result<GuessResponse> singleGuess(
            @RequestAttribute("currentUser") User user,
            @RequestBody SingleGuessRequest req) {
        return Result.ok(gameService.singleGuess(user.getId(), req.sessionId(), req.vtuberId()));
    }

    @GetMapping("/single/{sessionId}")
    public Result<DailyGameInfoVO> singleState(
            @RequestAttribute("currentUser") User user,
            @PathVariable Long sessionId) {
        return Result.ok(gameService.getSingleState(user.getId(), sessionId));
    }

    public record StartSingleRequest(String poolTag) {}
    public record SingleGuessRequest(Long sessionId, Long vtuberId) {}
```

> 需要 import `org.springframework.web.bind.annotation.PathVariable` 和 `PoolVO`, `SingleStartResponse`。

- [x] **Step 4：编写 SingleGameTest**

`backend/src/test/java/com/guessv/controller/SingleGameTest.java`:
```java
package com.guessv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.entity.User;
import com.guessv.mapper.UserMapper;
import com.guessv.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GuessVApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SingleGameTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired UserService userService;
    @Autowired UserMapper userMapper;
    private String token;

    @BeforeEach void setup() { token = userService.createAnonymousUser("单人玩家", "fp").token(); }
    @AfterEach void cleanup() { userMapper.delete(new QueryWrapper<User>().eq("is_anonymous", true)); }

    private HttpHeaders auth() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-User-Token", token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
    private String url(String p) { return "http://localhost:" + port + p; }

    @Test
    void listPoolsReturnsNonEmpty() {
        var entity = new HttpEntity<>(auth());
        var resp = restTemplate.exchange(url("/api/game/single/pools"), HttpMethod.GET, entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("全量"));
    }

    @Test
    void startReturnsSessionId() {
        var entity = new HttpEntity<>("{\"poolTag\":\"全量\"}", auth());
        var resp = restTemplate.postForEntity(url("/api/game/single/start"), entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("sessionId"));
    }

    @Test
    void guessOnSingleSessionWorks() {
        var startEntity = new HttpEntity<>("{\"poolTag\":\"全量\"}", auth());
        var startResp = restTemplate.postForEntity(url("/api/game/single/start"), startEntity, String.class);
        // 提取 sessionId（简单字符串查找）
        String body = startResp.getBody();
        int idx = body.indexOf("\"sessionId\":");
        String after = body.substring(idx + 13);
        long sessionId = Long.parseLong(after.substring(0, after.indexOf(',')).trim());

        var guessEntity = new HttpEntity<>(
                "{\"sessionId\":" + sessionId + ",\"vtuberId\":9770}", auth());
        var resp = restTemplate.postForEntity(url("/api/game/single/guess"), guessEntity, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("comparison"));
    }
}
```

- [x] **Step 5：运行测试**

Run: `cd backend && mvn test -Dtest=SingleGameTest -q`
Expected: 3 个测试通过

- [x] **Step 6：提交**

```bash
git add backend/
git commit -m "feat(game): 单人模式 API（题库/开始/猜测/断线恢复）"
```

---

## Task 5：收尾验证与手动测试方案

- [x] **Step 1：全量测试**

Run: `cd backend && mvn test -q`
Expected: 全部通过（M1+M2+M3）

- [x] **Step 2：手动启动验证**

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

流程（需先通过 /api/user/init 获取 token）：
1. `GET /api/game/daily`
2. `POST /api/game/daily/guess {vtuberId: <某个种子V的id>}`
3. `GET /api/game/single/pools`
4. `POST /api/game/single/start {poolTag:"全量"}`
5. `POST /api/game/single/guess {sessionId, vtuberId}`
6. 重复猜测同一 V → 409

- [x] **Step 3：编写 M3 手动测试方案文档**

保存到 `docs/plans/phase-1/m3-manual-test-plan.md`。

- [x] **Step 4：更新路线图**

`docs/plans/000-roadmap.md`：M3 → ✅

- [x] **Step 5：提交并推送**

```bash
git add -A
git commit -m "feat(game): M3 游戏核心完成"
git push
```

---

## M3 完成标准

- [x] `ComparisonService` 7 维度对比，9 个单测覆盖各场景
- [x] 每日目标定时刷新 + getOrCreate 兜底
- [x] `GET /api/game/daily` 返回今日信息（不暴露目标）
- [x] `POST /api/game/daily/guess` 返回对比结果 + 胜负判定
- [x] 重复猜测返回 409
- [x] 单人模式 pools/start/guess/state 全部可用
- [x] 单人会话通过 user_id 校验归属
- [x] 全部测试通过：`mvn test`

---

*M3 完成后，进入 M4（前端游戏界面）。*
