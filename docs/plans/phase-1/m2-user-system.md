# M2：用户系统 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> 返回 [路线图](../000-roadmap.md) | [AGENTS.md](../../../AGENTS.md)

**Goal:** 实现匿名用户体系：昵称+#游戏ID 注册、JWT 鉴权、个人信息 API。前端部分挪到 M4。

**Architecture:** 后端独立的用户模块。JWT 无状态鉴权 + HandlerInterceptor 拦截。昵称池 + 敏感词过滤 + 游戏 ID 生成。

**Tech Stack:** Spring Boot 3.2 + jjwt 0.12 + MyBatis-Plus + JUnit 5

## Global Constraints

- 沿用 M1 的包名 `com.guessv`、统一响应体 `Result<T>`、全局异常处理
- 用户凭证 Header：`X-User-Token`（JWT）
- JWT 库：`io.jsonwebtoken:jjwt` 0.12.6
- 游戏 ID：4 位，字符集 `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`（排除 I/O/0/1）
- 昵称规则：2-16 字符，禁止 `#`，敏感词过滤
- 测试隔离：Service 测试用 `@Transactional`；Controller 测试用真实 HTTP，`@AfterEach` 清理
- 提交信息格式：`feat(user): xxx`

## 设计文档参考

- [用户系统架构](../../architecture/003-user-system.md)
- [用户表设计](../../database/003-user-table.md)
- [用户 API 设计](../../api/002-user-api.md)

---

## Task 1：JWT 工具与依赖

**Files:**
- Modify: `backend/pom.xml`（加 jjwt 依赖）
- Modify: `backend/src/main/resources/application.yml`（加 jwt 配置）
- Create: `backend/src/main/java/com/guessv/util/JwtUtil.java`
- Create: `backend/src/test/java/com/guessv/util/JwtUtilTest.java`

**Interfaces:**
- Produces: `JwtUtil.generate(userId, nickname, gameId, anonymous)` → token 字符串
- Produces: `JwtUtil.parse(token)` → Claims
- Produces: `JwtUtil.isValid(token)` → boolean

- [x] **Step 1：pom.xml 加 jjwt 依赖**

在 `<properties>` 中加：
```xml
<jjwt.version>0.12.6</jjwt.version>
```

在 `<dependencies>` 中加：
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
```

- [x] **Step 2：application.yml 加 JWT 配置**

在 `app:` 节点下追加：
```yaml
  jwt:
    secret: ${JWT_SECRET:guessv-dev-secret-key-please-change-in-production-at-least-32-chars-long}
    expiration-hours: 720
```

- [x] **Step 3：先写 JwtUtilTest（TDD）**

`backend/src/test/java/com/guessv/util/JwtUtilTest.java`:
```java
package com.guessv.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JwtUtilTest {

    @Autowired private JwtUtil jwtUtil;

    @Test
    void generateAndParseRoundTrip() {
        String token = jwtUtil.generate("user-uuid-123", "小明", "AB12", true);
        Claims claims = jwtUtil.parse(token);
        assertEquals("user-uuid-123", claims.getSubject());
        assertEquals("小明", claims.get("nickname"));
        assertEquals("AB12", claims.get("gameId"));
        assertEquals(true, claims.get("anonymous"));
    }

    @Test
    void isValidReturnsTrueForValidToken() {
        String token = jwtUtil.generate("u1", "n", "G1", false);
        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    void isValidReturnsFalseForGarbage() {
        assertFalse(jwtUtil.isValid("not.a.jwt"));
        assertFalse(jwtUtil.isValid(""));
    }
}
```

- [x] **Step 4：实现 JwtUtil**

`backend/src/main/java/com/guessv/util/JwtUtil.java`:
```java
package com.guessv.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-hours}")
    private long expirationHours;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(String userId, String nickname, String gameId, boolean anonymous) {
        return Jwts.builder()
                .subject(userId)
                .claim("nickname", nickname)
                .claim("gameId", gameId)
                .claim("anonymous", anonymous)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(expirationHours, ChronoUnit.HOURS)))
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [x] **Step 5：运行测试**

Run: `cd backend && mvn test -Dtest=JwtUtilTest -q`
Expected: 3 个测试通过

- [x] **Step 6：提交**

```bash
git add backend/
git commit -m "feat(user): JWT 工具与依赖"
```

---

## Task 2：昵称池与随机昵称 API

**Files:**
- Create: `backend/src/main/resources/nicknames.json`
- Create: `backend/src/main/resources/sensitive-words.txt`
- Create: `backend/src/main/java/com/guessv/service/NicknameService.java`
- Create: `backend/src/main/java/com/guessv/controller/UserController.java`
- Create: `backend/src/test/java/com/guessv/service/NicknameServiceTest.java`

**Interfaces:**
- Produces: `NicknameService.generateRandom()` → 随机昵称
- Produces: `NicknameService.filter(name)` → 过滤后的昵称（含敏感词检测）
- Produces: `GET /api/user/nickname/random?count=5`

- [x] **Step 1：创建昵称池**

`backend/src/main/resources/nicknames.json`:
```json
{
  "adjectives": ["快乐的", "可爱的", "神秘的", "勤奋的", "慵懒的", "勇敢的", "害羞的", "贪吃的", "爱睡的", "电波系"],
  "nouns": ["小猫咪", "单推人", "DD头子", "gachi恋", "观测者", "虾虾", "烤肉man", "直播民", "同传man", "图床君"],
  "presets": ["单推人", "DD头子", "观测者", "烤肉man", "直播民", "纯路人", "热心观众", "潜水员"]
}
```

- [x] **Step 2：创建敏感词词库**

`backend/src/main/resources/sensitive-words.txt`（每行一个，UTF-8）:
```
政治
色情
赌博
诈骗
毒品
反动
犯罪
暴力
恐怖
违禁
```
> MVP 用小词库，生产环境替换为完整词库。

- [x] **Step 3：先写 NicknameServiceTest（TDD）**

`backend/src/test/java/com/guessv/service/NicknameServiceTest.java`:
```java
package com.guessv.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class NicknameServiceTest {

    @Autowired private NicknameService nicknameService;

    @Test
    void generateRandomReturnsNonEmpty() {
        String name = nicknameService.generateRandom();
        assertNotNull(name);
        assertFalse(name.isBlank());
        assertTrue(name.length() >= 2 && name.length() <= 16);
    }

    @Test
    void generateRandomDoesNotContainSensitive() {
        for (int i = 0; i < 50; i++) {
            String name = nicknameService.generateRandom();
            assertFalse(nicknameService.containsSensitive(name), "生成昵称含敏感词: " + name);
        }
    }

    @Test
    void containsSensitiveDetectsBanned() {
        assertTrue(nicknameService.containsSensitive("赌博大王"));
        assertTrue(nicknameService.containsSensitive("色情主播"));
    }

    @Test
    void containsSensitiveAllowsClean() {
        assertFalse(nicknameService.containsSensitive("小明"));
        assertFalse(nicknameService.containsSensitive("Gura单推"));
    }

    @Test
    void validateRejectsTooLong() {
        var r = nicknameService.validate("这是一个超过十六个字符的昵称真的太长了");
        assertFalse(r.valid());
        assertEquals("length", r.reason());
    }

    @Test
    void validateRejectsHash() {
        var r = nicknameService.validate("小明#AB12");
        assertFalse(r.valid());
        assertEquals("format", r.reason());
    }

    @Test
    void validateRejectsSensitive() {
        var r = nicknameService.validate("赌博王");
        assertFalse(r.valid());
        assertEquals("sensitive", r.reason());
    }

    @Test
    void validateAcceptsClean() {
        var r = nicknameService.validate("小明");
        assertTrue(r.valid());
        assertNull(r.reason());
    }
}
```

- [x] **Step 4：实现 NicknameService**

`backend/src/main/java/com/guessv/service/NicknameService.java`:
```java
package com.guessv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class NicknameService {

    private final ObjectMapper objectMapper;
    private List<String> adjectives = List.of();
    private List<String> nouns = List.of();
    private List<String> presets = List.of();
    private List<String> sensitiveWords = List.of();

    @PostConstruct
    void init() {
        try (InputStream is = new ClassPathResource("nicknames.json").getInputStream()) {
            JsonNode node = objectMapper.readTree(is);
            adjectives = toList(node.get("adjectives"));
            nouns = toList(node.get("nouns"));
            presets = toList(node.get("presets"));
        } catch (Exception e) {
            log.warn("加载昵称池失败: {}", e.getMessage());
        }
        try (InputStream is = new ClassPathResource("sensitive-words.txt").getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            sensitiveWords = content.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } catch (Exception e) {
            log.warn("加载敏感词库失败: {}", e.getMessage());
        }
        log.info("昵称池加载完成：形容词 {} / 名词 {} / 预设 {} / 敏感词 {}",
                adjectives.size(), nouns.size(), presets.size(), sensitiveWords.size());
    }

    private List<String> toList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> list = new ArrayList<>();
        node.forEach(n -> list.add(n.asText()));
        return list;
    }

    public String generateRandom() {
        Random r = ThreadLocalRandom.current();
        // 一半概率用预设，一半概率组合
        if (!presets.isEmpty() && r.nextBoolean()) {
            return presets.get(r.nextInt(presets.size()));
        }
        if (!adjectives.isEmpty() && !nouns.isEmpty()) {
            return adjectives.get(r.nextInt(adjectives.size()))
                    + nouns.get(r.nextInt(nouns.size()));
        }
        return "玩家" + r.nextInt(1000, 9999);
    }

    public boolean containsSensitive(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        for (String w : sensitiveWords) {
            if (lower.contains(w.toLowerCase())) return true;
        }
        return false;
    }

    /**
     * 校验昵称合法性。
     * @return valid=true 表示通过；否则 reason 为 length/format/sensitive
     */
    public ValidationResult validate(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return new ValidationResult(false, "length");
        }
        int len = nickname.trim().length();
        if (len < 2 || len > 16) {
            return new ValidationResult(false, "length");
        }
        if (nickname.contains("#")) {
            return new ValidationResult(false, "format");
        }
        if (containsSensitive(nickname)) {
            return new ValidationResult(false, "sensitive");
        }
        return new ValidationResult(true, null);
    }

    public record ValidationResult(boolean valid, String reason) {}
}
```

- [x] **Step 5：实现 UserController（先只放昵称接口）**

`backend/src/main/java/com/guessv/controller/UserController.java`:
```java
package com.guessv.controller;

import com.guessv.common.Result;
import com.guessv.service.NicknameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final NicknameService nicknameService;

    @GetMapping("/nickname/random")
    public Result<List<String>> randomNicknames(@RequestParam(defaultValue = "5") int count) {
        if (count <= 0 || count > 10) count = 5;
        List<String> names = IntStream.range(0, count)
                .mapToObj(i -> nicknameService.generateRandom())
                .toList();
        return Result.ok(names);
    }
}
```

- [x] **Step 6：运行测试**

Run: `cd backend && mvn test -Dtest=NicknameServiceTest -q`
Expected: 7 个测试通过

- [x] **Step 7：提交**

```bash
git add backend/
git commit -m "feat(user): 昵称池、敏感词过滤与随机昵称 API"
```

---

## Task 3：用户初始化 API

**Files:**
- Create: `backend/src/main/java/com/guessv/service/UserService.java`
- Create: `backend/src/main/java/com/guessv/dto/UserInitRequest.java`
- Create: `backend/src/main/java/com/guessv/dto/UserInitResponse.java`
- Modify: `backend/src/main/java/com/guessv/controller/UserController.java`（加 POST /init）
- Create: `backend/src/test/java/com/guessv/service/UserServiceTest.java`

**Interfaces:**
- Produces: `UserService.createAnonymousUser(nickname, fingerprint)` → User + gameId + token
- Produces: `POST /api/user/init`

- [x] **Step 1：先写 UserServiceTest（TDD）**

`backend/src/test/java/com/guessv/service/UserServiceTest.java`:
```java
package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.entity.User;
import com.guessv.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GuessVApplication.class)
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired private UserService userService;
    @Autowired private UserMapper userMapper;

    @Test
    void createAnonymousUserWithCustomNickname() {
        var resp = userService.createAnonymousUser("小明", "fp_abc");
        assertNotNull(resp.userId());
        assertEquals("小明", resp.nickname());
        assertNotNull(resp.gameId());
        assertEquals(4, resp.gameId().length());
        assertEquals("小明#" + resp.gameId(), resp.displayName());
        assertTrue(resp.isAnonymous());
        assertNotNull(resp.token());

        User saved = userMapper.selectOne(new QueryWrapper<User>().eq("uuid", resp.userId()));
        assertNotNull(saved);
        assertEquals("小明", saved.getNickname());
        assertEquals(resp.gameId(), saved.getGameId());
        assertTrue(saved.getIsAnonymous());
    }

    @Test
    void createAnonymousUserWithRandomNickname() {
        var resp = userService.createAnonymousUser(null, "fp_xyz");
        assertNotNull(resp.nickname());
        assertFalse(resp.nickname().isBlank());
    }

    @Test
    void createRejectsSensitiveNickname() {
        assertThrows(RuntimeException.class, () ->
                userService.createAnonymousUser("赌博王", "fp_1"));
    }

    @Test
    void gameIdIsUnique() {
        var a = userService.createAnonymousUser("甲", "fp1");
        var b = userService.createAnonymousUser("乙", "fp2");
        assertNotEquals(a.gameId(), b.gameId());
    }
}
```

- [x] **Step 2：实现 DTO**

`backend/src/main/java/com/guessv/dto/UserInitRequest.java`:
```java
package com.guessv.dto;

import jakarta.validation.constraints.Size;

public record UserInitRequest(
        @Size(min = 2, max = 16) String nickname,
        Boolean useRandomNickname,
        String deviceFingerprint
) {}
```

`backend/src/main/java/com/guessv/dto/UserInitResponse.java`:
```java
package com.guessv.dto;

public record UserInitResponse(
        String userId,
        String nickname,
        String gameId,
        String displayName,
        String token,
        boolean isAnonymous
) {}
```

- [x] **Step 3：实现 UserService**

`backend/src/main/java/com/guessv/service/UserService.java`:
```java
package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.common.BizException;
import com.guessv.dto.UserInitResponse;
import com.guessv.entity.User;
import com.guessv.mapper.UserMapper;
import com.guessv.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String GAME_ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int GAME_ID_LENGTH = 4;
    private static final int GAME_ID_MAX_RETRY = 10;

    private final UserMapper userMapper;
    private final NicknameService nicknameService;
    private final JwtUtil jwtUtil;

    public UserInitResponse createAnonymousUser(String nickname, String deviceFingerprint) {
        // 昵称处理：空则随机生成
        if (nickname == null || nickname.isBlank()) {
            nickname = nicknameService.generateRandom();
        }
        // 校验昵称
        var validation = nicknameService.validate(nickname);
        if (!validation.valid()) {
            String msg = switch (validation.reason()) {
                case "length" -> "昵称长度需为 2-16 字符";
                case "format" -> "昵称格式不合法（禁止使用 #）";
                case "sensitive" -> "昵称包含敏感词，请更换";
                default -> "昵称不合法";
            };
            throw new BizException(400, msg);
        }

        // 生成游戏 ID（确保唯一）
        String gameId = generateUniqueGameId();

        // 创建用户
        User user = new User();
        user.setUuid(UUID.randomUUID().toString());
        user.setNickname(nickname);
        user.setGameId(gameId);
        user.setDeviceFingerprint(deviceFingerprint);
        user.setIsAnonymous(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastActiveAt(LocalDateTime.now());
        userMapper.insert(user);

        // 生成 token
        String token = jwtUtil.generate(user.getUuid(), nickname, gameId, true);

        log.info("创建匿名用户：{}#{}", nickname, gameId);
        return new UserInitResponse(
                user.getUuid(),
                nickname,
                gameId,
                nickname + "#" + gameId,
                token,
                true
        );
    }

    private String generateUniqueGameId() {
        for (int attempt = 0; attempt < GAME_ID_MAX_RETRY; attempt++) {
            StringBuilder sb = new StringBuilder(GAME_ID_LENGTH);
            for (int i = 0; i < GAME_ID_LENGTH; i++) {
                int idx = (int) (Math.random() * GAME_ID_CHARS.length());
                sb.append(GAME_ID_CHARS.charAt(idx));
            }
            String candidate = sb.toString();
            long exists = userMapper.selectCount(
                    new QueryWrapper<User>().eq("game_id", candidate));
            if (exists == 0) {
                return candidate;
            }
        }
        // 兜底：扩展到 5 位（极端情况下 4 位空间耗尽）
        return GAME_ID_CHARS.charAt((int)(Math.random()*GAME_ID_CHARS.length()))
                + generateUniqueGameId();
    }
}
```

- [x] **Step 4：UserController 加 /init 端点**

修改 `UserController.java`，在类中注入 `UserService` 并添加：
```java
    private final UserService userService;

    @PostMapping("/init")
    public Result<UserInitResponse> init(@Valid @RequestBody UserInitRequest req) {
        String nickname = Boolean.TRUE.equals(req.useRandomNickname()) ? null : req.nickname();
        var resp = userService.createAnonymousUser(nickname, req.deviceFingerprint());
        return Result.ok(resp);
    }
```

完整文件（替换原 UserController）：
```java
package com.guessv.controller;

import com.guessv.common.Result;
import com.guessv.dto.UserInitRequest;
import com.guessv.dto.UserInitResponse;
import com.guessv.service.NicknameService;
import com.guessv.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final NicknameService nicknameService;
    private final UserService userService;

    @GetMapping("/nickname/random")
    public Result<List<String>> randomNicknames(@RequestParam(defaultValue = "5") int count) {
        if (count <= 0 || count > 10) count = 5;
        List<String> names = IntStream.range(0, count)
                .mapToObj(i -> nicknameService.generateRandom())
                .toList();
        return Result.ok(names);
    }

    @PostMapping("/init")
    public Result<UserInitResponse> init(@Valid @RequestBody UserInitRequest req) {
        String nickname = Boolean.TRUE.equals(req.useRandomNickname()) ? null : req.nickname();
        var resp = userService.createAnonymousUser(nickname, req.deviceFingerprint());
        return Result.ok(resp);
    }
}
```

> 注意构造注入顺序：Lombok 按字段声明顺序生成构造参数，`nicknameService` 在前 `userService` 在后，Spring 按类型注入，无序依赖问题。

- [x] **Step 5：运行测试**

Run: `cd backend && mvn test -Dtest=UserServiceTest -q`
Expected: 4 个测试通过

- [x] **Step 6：提交**

```bash
git add backend/
git commit -m "feat(user): 用户初始化 API（昵称+#游戏ID+JWT）"
```

---

## Task 4：鉴权拦截器

**Files:**
- Create: `backend/src/main/java/com/guessv/config/AuthInterceptor.java`
- Create: `backend/src/main/java/com/guessv/config/WebMvcConfig.java`
- Create: `backend/src/test/java/com/guessv/controller/UserAuthTest.java`

**Interfaces:**
- Produces: 全局鉴权拦截，受保护接口需要 `X-User-Token` Header
- Produces: 请求属性 `userId` / `currentUser` 供 Controller 使用
- 白名单：`/api/health`、`/api/user/init`、`/api/user/nickname/random`、`/api/admin/**`（M5 再加 admin）

- [x] **Step 1：实现 AuthInterceptor**

`backend/src/main/java/com/guessv/config/AuthInterceptor.java`:
```java
package com.guessv.config;

import com.guessv.common.BizException;
import com.guessv.entity.User;
import com.guessv.mapper.UserMapper;
import com.guessv.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_CURRENT_USER = "currentUser";

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        String token = req.getHeader("X-User-Token");
        if (token == null || token.isBlank()) {
            throw new BizException(401, "未提供用户凭证");
        }
        if (!jwtUtil.isValid(token)) {
            throw new BizException(401, "用户凭证无效或已过期");
        }
        Claims claims = jwtUtil.parse(token);
        String userId = claims.getSubject();
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("uuid", userId));
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        req.setAttribute(ATTR_USER_ID, user.getUuid());
        req.setAttribute(ATTR_CURRENT_USER, user);
        // 刷新最后活跃时间（容错：失败不影响请求）
        try {
            user.setLastActiveAt(java.time.LocalDateTime.now());
            userMapper.updateById(user);
        } catch (Exception ignored) {}
        return true;
    }
}
```

- [x] **Step 2：实现 WebMvcConfig 注册拦截器与白名单**

`backend/src/main/java/com/guessv/config/WebMvcConfig.java`:
```java
package com.guessv.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/user/init",
                        "/api/user/nickname/random",
                        "/api/vtuber/search",
                        "/api/admin/**"
                );
    }
}
```

> 注：`/api/vtuber/search` 暂不鉴权（搜索可匿名），M3 游戏接口再按需收紧。

- [x] **Step 3：编写鉴权测试（真实 HTTP）**

`backend/src/test/java/com/guessv/controller/UserAuthTest.java`:
```java
package com.guessv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.entity.User;
import com.guessv.mapper.UserMapper;
import com.guessv.service.UserService;
import org.junit.jupiter.api.AfterEach;
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
class UserAuthTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired UserService userService;
    @Autowired UserMapper userMapper;

    @AfterEach
    void cleanup() {
        userMapper.delete(new QueryWrapper<User>().eq("is_anonymous", true));
    }

    private String url(String p) { return "http://localhost:" + port + p; }

    @Test
    void initEndpointIsPublic() {
        var req = new org.springframework.http.HttpEntity<>(
                "{\"nickname\":\"测试用户\",\"deviceFingerprint\":\"fp\"}",
                MediaType.APPLICATION_JSON);
        var resp = restTemplate.postForEntity(url("/api/user/init"), req, String.class);
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("测试用户#"));
    }

    @Test
    void protectedEndpointRejectsMissingToken() {
        // /api/user/profile 受保护
        var resp = restTemplate.getForEntity(url("/api/user/profile"), String.class);
        // 全局异常处理返回 200 + code 401（无 ResponseStatus）
        assertTrue(resp.getBody().contains("\"code\":401"));
    }

    @Test
    void protectedEndpointRejectsInvalidToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Token", "invalid.token.here");
        var entity = new HttpEntity<>(headers);
        var resp = restTemplate.exchange(url("/api/user/profile"), HttpMethod.GET, entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":401"));
    }

    @Test
    void protectedEndpointAcceptsValidToken() {
        var init = userService.createAnonymousUser("鉴权测试", "fp");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Token", init.token());
        var entity = new HttpEntity<>(headers);
        var resp = restTemplate.exchange(url("/api/user/profile"), HttpMethod.GET, entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("鉴权测试"));
    }
}
```

> 本测试依赖 Task 5 的 `/api/user/profile`。先实现 Task 5 再运行；或临时跳过最后两个测试。

- [x] **Step 4：提交**

```bash
git add backend/
git commit -m "feat(user): JWT 鉴权拦截器与白名单配置"
```

---

## Task 5：个人信息与昵称管理 API

**Files:**
- Create: `backend/src/main/java/com/guessv/dto/UserProfileVO.java`
- Create: `backend/src/main/java/com/guessv/dto/NicknameCheckResponse.java`
- Modify: `backend/src/main/java/com/guessv/controller/UserController.java`
- Modify: `backend/src/main/java/com/guessv/service/UserService.java`
- Create: `backend/src/test/java/com/guessv/controller/UserProfileTest.java`

**Interfaces:**
- Produces: `GET /api/user/profile`（需鉴权）
- Produces: `POST /api/user/nickname/check`
- Produces: `PUT /api/user/nickname`（需鉴权）

- [x] **Step 1：实现 DTO**

`backend/src/main/java/com/guessv/dto/UserProfileVO.java`:
```java
package com.guessv.dto;

public record UserProfileVO(
        String userId,
        String nickname,
        String gameId,
        String displayName,
        boolean isAnonymous,
        String username,
        String avatarUrl,
        String createdAt
) {}
```

`backend/src/main/java/com/guessv/dto/NicknameCheckResponse.java`:
```java
package com.guessv.dto;

public record NicknameCheckResponse(
        boolean valid,
        String reason
) {}
```

`backend/src/main/java/com/guessv/dto/UpdateNicknameRequest.java`:
```java
package com.guessv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @NotBlank @Size(min = 2, max = 16) String nickname
) {}
```

- [x] **Step 2：UserService 加 profile / changeNickname**

在 `UserService.java` 中追加方法：
```java
    public UserProfileVO getProfile(User user) {
        return new UserProfileVO(
                user.getUuid(),
                user.getNickname(),
                user.getGameId(),
                user.getNickname() + "#" + user.getGameId(),
                user.getIsAnonymous(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }

    public NicknameCheckResponse checkNickname(String nickname) {
        var v = nicknameService.validate(nickname);
        return new NicknameCheckResponse(v.valid(), v.reason());
    }

    @org.springframework.transaction.annotation.Transactional
    public UserProfileVO changeNickname(User user, String newNickname) {
        var v = nicknameService.validate(newNickname);
        if (!v.valid()) {
            String msg = switch (v.reason()) {
                case "length" -> "昵称长度需为 2-16 字符";
                case "format" -> "昵称格式不合法（禁止使用 #）";
                case "sensitive" -> "昵称包含敏感词，请更换";
                default -> "昵称不合法";
            };
            throw new BizException(400, msg);
        }
        user.setNickname(newNickname);
        userMapper.updateById(user);
        return getProfile(user);
    }
```

并在类顶部加 import：`import com.guessv.dto.NicknameCheckResponse;` `import com.guessv.dto.UserProfileVO;`

- [x] **Step 3：UserController 加端点**

在 `UserController.java` 中追加：
```java
    @org.springframework.web.bind.annotation.GetMapping("/profile")
    public Result<UserProfileVO> profile(
            @org.springframework.web.bind.annotation.RequestAttribute("currentUser") User user) {
        return Result.ok(userService.getProfile(user));
    }

    @PostMapping("/nickname/check")
    public Result<NicknameCheckResponse> checkNickname(@RequestParam String nickname) {
        return Result.ok(userService.checkNickname(nickname));
    }

    @PutMapping("/nickname")
    public Result<UserProfileVO> updateNickname(
            @org.springframework.web.bind.annotation.RequestAttribute("currentUser") User user,
            @Valid @RequestBody UpdateNicknameRequest req) {
        return Result.ok(userService.changeNickname(user, req.nickname()));
    }
```

并在文件顶部加 import：
```java
import com.guessv.dto.UserProfileVO;
import com.guessv.dto.NicknameCheckResponse;
import com.guessv.dto.UpdateNicknameRequest;
import com.guessv.entity.User;
```

- [x] **Step 4：编写 UserProfileTest**

`backend/src/test/java/com/guessv/controller/UserProfileTest.java`:
```java
package com.guessv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.entity.User;
import com.guessv.mapper.UserMapper;
import com.guessv.service.UserService;
import org.junit.jupiter.api.AfterEach;
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
class UserProfileTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired UserService userService;
    @Autowired UserMapper userMapper;

    @AfterEach
    void cleanup() {
        userMapper.delete(new QueryWrapper<User>().eq("is_anonymous", true));
    }

    private HttpHeaders auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set("X-User-Token", token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private String url(String p) { return "http://localhost:" + port + p; }

    @Test
    void profileReturnsUserInfo() {
        var init = userService.createAnonymousUser("资料测试", "fp");
        var entity = new HttpEntity<>(auth(init.token()));
        var resp = restTemplate.exchange(url("/api/user/profile"), HttpMethod.GET, entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("资料测试"));
        assertTrue(resp.getBody().contains("\"isAnonymous\":true"));
    }

    @Test
    void checkNicknameRejectsSensitive() {
        var resp = restTemplate.getForObject(
                url("/api/user/nickname/check?nickname=赌博王"), String.class);
        assertTrue(resp.contains("\"valid\":false"));
        assertTrue(resp.contains("sensitive"));
    }

    @Test
    void checkNicknameAcceptsClean() {
        var resp = restTemplate.getForObject(
                url("/api/user/nickname/check?nickname=小明"), String.class);
        assertTrue(resp.contains("\"valid\":true"));
    }

    @Test
    void changeNicknameSucceeds() {
        var init = userService.createAnonymousUser("旧昵称", "fp");
        var entity = new HttpEntity<>("{\"nickname\":\"新昵称\"}", auth(init.token()));
        var resp = restTemplate.exchange(url("/api/user/nickname"), HttpMethod.PUT, entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("新昵称"));
    }

    @Test
    void changeNicknameRejectsSensitive() {
        var init = userService.createAnonymousUser("合法昵称", "fp");
        var entity = new HttpEntity<>("{\"nickname\":\"色情主播\"}", auth(init.token()));
        var resp = restTemplate.exchange(url("/api/user/nickname"), HttpMethod.PUT, entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":400"));
    }
}
```

- [x] **Step 5：运行全部测试**

Run: `cd backend && mvn test -q`
Expected: 全部通过

- [x] **Step 6：提交**

```bash
git add backend/
git commit -m "feat(user): 个人信息、昵称校验与修改 API"
```

---

## Task 6：收尾验证与手动测试

- [x] **Step 1：全量测试**

Run: `cd backend && mvn test -q`
Expected: 全部通过（M1 + M2）

- [x] **Step 2：手动启动验证**

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

另开终端：
```bash
# 1. 随机昵称
curl "http://localhost:8080/api/user/nickname/random?count=5"

# 2. 初始化用户
curl -X POST http://localhost:8080/api/user/init \
  -H "Content-Type: application/json" \
  -d '{"nickname":"小明","deviceFingerprint":"fp_test"}'

# 3. 用返回的 token 查 profile（替换 <TOKEN>）
curl http://localhost:8080/api/user/profile -H "X-User-Token: <TOKEN>"

# 4. 昵称校验
curl "http://localhost:8080/api/user/nickname/check?nickname=赌博王"

# 5. 修改昵称
curl -X PUT http://localhost:8080/api/user/nickname \
  -H "Content-Type: application/json" \
  -H "X-User-Token: <TOKEN>" \
  -d '{"nickname":"新昵称"}'

# 6. 鉴权失败（无 token）
curl http://localhost:8080/api/user/profile
```

- [x] **Step 3：更新路线图**

修改 `docs/plans/000-roadmap.md`：
- M2 状态 → ✅
- 当前任务 → M2 完成

- [x] **Step 4：提交并推送**

```bash
git add -A
git commit -m "chore(user): M2 收尾，更新路线图"
git push
```

---

## M2 完成标准

- [x] `POST /api/user/init` 能创建匿名用户并返回 JWT
- [x] 游戏 ID 4 位且全局唯一
- [x] 敏感词昵称被拒绝（含 400 错误）
- [x] `GET /api/user/profile` 需鉴权，无 token 返回 401
- [x] `PUT /api/user/nickname` 能修改昵称（含敏感词校验）
- [x] `GET /api/user/nickname/random` 无需鉴权
- [x] 全部测试通过：`mvn test`

---

*M2 完成后，进入 M3（游戏核心）。*
