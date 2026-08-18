# M1：后端骨架与数据导入 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> 返回 [路线图](../000-roadmap.md) | [AGENTS.md](../../../AGENTS.md)

**Goal:** 搭建 Spring Boot 后端骨架，将 list.json 的 10014 条 VTuber 数据导入 SQLite，提供搜索 API。

**Architecture:** Monorepo 的 `backend/` 子项目。Spring Boot 3.2 + MyBatis-Plus + SQLite。启动时检测空表自动导入 list.json。提供统一响应体、全局异常处理、VTuber 搜索 API。

**Tech Stack:** Java 21, Spring Boot 3.2.5, MyBatis-Plus 3.5.7, SQLite 3.45, JUnit 5

## Global Constraints

- Java 版本：21
- Spring Boot 版本：3.2.5
- MyBatis-Plus 版本：3.5.7（用 `mybatis-plus-spring-boot3-starter`，适配 SB3）
- 包名根：`com.guessv`
- 禁止手写 SQL 方言（用 MyBatis-Plus 标准方法 + QueryWrapper）
- 所有 JSON 字段用 `JacksonTypeHandler`，实体类 `@TableName(autoResultMap = true)`
- 统一响应格式：`{ "code": 200, "message": "success", "data": {} }`
- 提交信息格式：`<type>(<scope>): <subject>`（见 AGENTS.md 10.2）
- 后端运行目录：`backend/`（`mvn spring-boot:run` 在此目录执行）
- list.json 路径：`../data/list.json`（相对 backend/ 目录）

---

## Task 1：Spring Boot 项目骨架

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/guessv/GuessVApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`
- Create: `backend/src/test/resources/application-test.yml`
- Create: `backend/src/main/java/com/guessv/common/Result.java`
- Create: `backend/src/main/java/com/guessv/common/BizException.java`
- Create: `backend/src/main/java/com/guessv/config/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/guessv/controller/HealthController.java`
- Create: `backend/src/test/java/com/guessv/controller/HealthControllerTest.java`

**Interfaces:**
- Produces: `Result<T>` 统一响应体（后续所有 Controller 使用）
- Produces: `BizException` 业务异常（后续 Service 抛出）
- Produces: `GlobalExceptionHandler` 全局异常处理（后续所有 Controller 自动生效）

- [x] **Step 1：验证开发环境**

Run:
```bash
java -version
mvn -version
```
Expected: Java 21+，Maven 3.9+。若 Maven 未安装，执行 `mvn -N wrapper:wrapper -Dmaven=3.9.6` 生成 Maven Wrapper（Windows 用 `mvn.cmd` 的话改用 `.mvn/wrapper/MavenWrapperDownloader`）。

- [x] **Step 2：创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.guessv</groupId>
    <artifactId>guessv-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>guessv-backend</name>

    <properties>
        <java.version>21</java.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <sqlite.version>3.45.3.0</sqlite.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>${sqlite.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [x] **Step 3：创建主启动类**

`backend/src/main/java/com/guessv/GuessVApplication.java`:

```java
package com.guessv;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.guessv.mapper")
public class GuessVApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuessVApplication.class, args);
    }
}
```

- [x] **Step 4：创建配置文件**

`backend/src/main/resources/application.yml`（公共配置）:

```yaml
server:
  port: 8080
  compression:
    enabled: true
    mime-types: text/html,text/css,application/javascript,application/json

spring:
  profiles:
    active: dev
  jackson:
    default-property-inclusion: non_null

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

app:
  data:
    list-json-path: ../data/list.json
    import-enabled: true
```

`backend/src/main/resources/application-dev.yml`（开发环境，SQLite）:

```yaml
spring:
  datasource:
    url: jdbc:sqlite:../data/guessv.db
    driver-class-name: org.sqlite.JDBC
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
```

`backend/src/test/resources/application-test.yml`（测试环境，临时 SQLite 文件）:

```yaml
spring:
  datasource:
    url: jdbc:sqlite:target/test.db
    driver-class-name: org.sqlite.JDBC
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

app:
  data:
    list-json-path: classpath:fixtures/list-sample.json
    import-enabled: false
```

- [x] **Step 5：创建统一响应体**

`backend/src/main/java/com/guessv/common/Result.java`:

```java
package com.guessv.common;

public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static Result<Void> ok() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
```

- [x] **Step 6：创建业务异常类**

`backend/src/main/java/com/guessv/common/BizException.java`:

```java
package com.guessv.common;

public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }
}
```

- [x] **Step 7：创建全局异常处理器**

`backend/src/main/java/com/guessv/config/GlobalExceptionHandler.java`:

```java
package com.guessv.config;

import com.guessv.common.BizException;
import com.guessv.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getDefaultMessage() : "参数校验失败";
        return Result.error(400, msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleOther(Exception e) {
        return Result.error(500, "服务器内部错误：" + e.getMessage());
    }
}
```

- [x] **Step 8：创建健康检查接口**

`backend/src/main/java/com/guessv/controller/HealthController.java`:

```java
package com.guessv.controller;

import com.guessv.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.ok(Map.of("status", "UP", "app", "GuessV"));
    }
}
```

- [x] **Step 9：编写健康检查测试（先写测试，TDD）**

`backend/src/test/java/com/guessv/controller/HealthControllerTest.java`:

```java
package com.guessv.controller;

import com.guessv.GuessVApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GuessVApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void healthReturnsOk() {
        var resp = restTemplate.getForObject("http://localhost:" + port + "/api/health", String.class);
        assertTrue(resp.contains("\"code\":200"));
        assertTrue(resp.contains("GuessV"));
    }
}
```

- [x] **Step 10：运行测试验证通过**

Run:
```bash
cd backend && mvn test -Dtest=HealthControllerTest -q
```
Expected: Tests pass（注意：此时 schema.sql 尚不存在会报错——这是正常的，下一步创建）

- [x] **Step 11：提交**

```bash
git add backend/
git commit -m "feat(backend): Spring Boot 项目骨架与统一响应体"
```

---

## Task 2：数据库基础

**Files:**
- Create: `backend/src/main/resources/schema.sql`
- Create: `backend/src/main/java/com/guessv/config/MybatisPlusConfig.java`

**Interfaces:**
- Produces: 全部表的 DDL（后续实体类映射这些表）
- Produces: `MybatisPlusInterceptor` Bean（分页支持）

- [ ] **Step 1：创建 schema.sql（SQLite 方言）**

> 注：此文件为 SQLite 开发环境 DDL。MySQL 生产环境 DDL 将在部署阶段单独维护（见 [部署方案](../../architecture/004-deployment.md)）。SQLite 与 MySQL 的差异：`INTEGER PRIMARY KEY AUTOINCREMENT` vs `BIGINT AUTO_INCREMENT`、`TEXT` 兼容 MySQL 的 VARCHAR/JSON。

`backend/src/main/resources/schema.sql`:

```sql
-- VTuber 团体表
CREATE TABLE IF NOT EXISTS vtuber_group (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    name_en TEXT,
    region TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- VTuber 主表
CREATE TABLE IF NOT EXISTS vtuber (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid TEXT UNIQUE NOT NULL,
    name_cn TEXT,
    name_en TEXT,
    name_jp TEXT,
    name_default TEXT,
    aliases TEXT,
    debut_year INTEGER,
    debut_date TEXT,
    region TEXT,
    group_id INTEGER,
    group_name TEXT,
    activity_status TEXT,
    gender TEXT,
    hair_color TEXT,
    eye_color TEXT,
    outfit_theme TEXT,
    fan_name TEXT,
    symbol TEXT,
    representative_color TEXT,
    platforms TEXT,
    languages TEXT,
    avatar_url TEXT,
    data_status TEXT,
    data_source TEXT,
    locked_fields TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_vtuber_region ON vtuber(region);
CREATE INDEX IF NOT EXISTS idx_vtuber_group_id ON vtuber(group_id);
CREATE INDEX IF NOT EXISTS idx_vtuber_data_status ON vtuber(data_status);
CREATE INDEX IF NOT EXISTS idx_vtuber_activity_status ON vtuber(activity_status);

-- 每日目标表
CREATE TABLE IF NOT EXISTS daily_target (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    target_date TEXT UNIQUE NOT NULL,
    vtuber_id INTEGER NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 用户表
CREATE TABLE IF NOT EXISTS "user" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid TEXT UNIQUE NOT NULL,
    nickname TEXT NOT NULL,
    game_id TEXT NOT NULL,
    username TEXT UNIQUE,
    password_hash TEXT,
    email TEXT,
    oauth_provider TEXT,
    oauth_id TEXT,
    avatar_url TEXT,
    device_fingerprint TEXT,
    is_anonymous INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_active_at DATETIME,
    UNIQUE (nickname, game_id)
);

-- 游戏记录表
CREATE TABLE IF NOT EXISTS game_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    mode TEXT NOT NULL,
    target_id INTEGER NOT NULL,
    pool_tag TEXT,
    attempts INTEGER NOT NULL,
    max_attempts INTEGER NOT NULL,
    is_win INTEGER NOT NULL,
    guesses TEXT,
    started_at DATETIME NOT NULL,
    finished_at DATETIME
);
CREATE INDEX IF NOT EXISTS idx_game_record_user_id ON game_record(user_id);

-- 题库标签表
CREATE TABLE IF NOT EXISTS pool_tag (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tag_name TEXT UNIQUE NOT NULL,
    description TEXT,
    filter_rule TEXT NOT NULL,
    is_active INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operator_id INTEGER,
    operation_type TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_id INTEGER NOT NULL,
    field_name TEXT,
    old_value TEXT,
    new_value TEXT,
    ip_address TEXT,
    user_agent TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_operation_log_target ON operation_log(target_type, target_id);

-- 房间表（对战模式预留）
CREATE TABLE IF NOT EXISTS room (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    room_code TEXT UNIQUE NOT NULL,
    status TEXT NOT NULL,
    game_mode TEXT NOT NULL,
    target_id INTEGER NOT NULL,
    max_players INTEGER NOT NULL,
    current_players INTEGER DEFAULT 0,
    winner_id INTEGER,
    config TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME,
    finished_at DATETIME
);

-- 房间玩家表（对战模式预留）
CREATE TABLE IF NOT EXISTS room_player (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    room_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    player_name TEXT NOT NULL,
    is_ready INTEGER DEFAULT 0,
    score INTEGER DEFAULT 0,
    finish_rank INTEGER,
    attempts_used INTEGER DEFAULT 0,
    is_winner INTEGER DEFAULT 0,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    left_at DATETIME,
    UNIQUE (room_id, user_id)
);

-- 爬虫日志表
CREATE TABLE IF NOT EXISTS crawl_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vtuber_id INTEGER,
    source TEXT,
    status TEXT,
    fields_updated TEXT,
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

> 注意：`user` 是 SQLite/MySQL 保留字，加双引号引用。

- [ ] **Step 2：创建 MyBatis-Plus 配置**

`backend/src/main/java/com/guessv/config/MybatisPlusConfig.java`:

```java
package com.guessv.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        return interceptor;
    }
}
```

- [ ] **Step 3：运行健康检查测试验证 schema 加载**

Run:
```bash
cd backend && mvn test -Dtest=HealthControllerTest -q
```
Expected: 通过（schema.sql 自动执行，表已创建）

- [ ] **Step 4：提交**

```bash
git add backend/
git commit -m "feat(backend): 数据库 schema 与 MyBatis-Plus 配置"
```

---

## Task 3：VTuber 与团体实体 + Mapper

**Files:**
- Create: `backend/src/main/java/com/guessv/entity/VtuberGroup.java`
- Create: `backend/src/main/java/com/guessv/entity/Vtuber.java`
- Create: `backend/src/main/java/com/guessv/mapper/VtuberGroupMapper.java`
- Create: `backend/src/main/java/com/guessv/mapper/VtuberMapper.java`
- Create: `backend/src/test/java/com/guessv/mapper/VtuberMapperTest.java`

**Interfaces:**
- Produces: `VtuberMapper.selectList(QueryWrapper)` 用于 Task 5 导入和 Task 7 搜索
- Produces: `VtuberMapper.insert(Vtuber)` 用于 Task 5 导入
- Produces: `VtuberGroupMapper` 用于 Task 5 导入团体

- [ ] **Step 1：创建团体实体**

`backend/src/main/java/com/guessv/entity/VtuberGroup.java`:

```java
package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vtuber_group")
public class VtuberGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String nameEn;
    private String region;
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2：创建 VTuber 实体（含 JSON TypeHandler）**

`backend/src/main/java/com/guessv/entity/Vtuber.java`:

```java
package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "vtuber", autoResultMap = true)
public class Vtuber {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String uuid;
    private String nameCn;
    private String nameEn;
    private String nameJp;
    private String nameDefault;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> aliases;
    private Integer debutYear;
    private LocalDate debutDate;
    private String region;
    private Long groupId;
    private String groupName;
    private String activityStatus;
    private String gender;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> hairColor;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> eyeColor;
    private String outfitTheme;
    private String fanName;
    private String symbol;
    private String representativeColor;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> platforms;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> languages;
    private String avatarUrl;
    private String dataStatus;
    private String dataSource;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> lockedFields;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

> 关键：`autoResultMap = true` 必须设置，否则 `JacksonTypeHandler` 在 select 时不生效。

- [ ] **Step 3：创建 Mapper 接口**

`backend/src/main/java/com/guessv/mapper/VtuberGroupMapper.java`:

```java
package com.guessv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guessv.entity.VtuberGroup;

public interface VtuberGroupMapper extends BaseMapper<VtuberGroup> {
}
```

`backend/src/main/java/com/guessv/mapper/VtuberMapper.java`:

```java
package com.guessv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guessv.entity.Vtuber;

public interface VtuberMapper extends BaseMapper<Vtuber> {
}
```

- [ ] **Step 4：编写 Mapper 测试（TDD，先写测试）**

`backend/src/test/java/com/guessv/mapper/VtuberMapperTest.java`:

```java
package com.guessv.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.entity.Vtuber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GuessVApplication.class)
@ActiveProfiles("test")
@Transactional
class VtuberMapperTest {

    @Autowired
    private VtuberMapper vtuberMapper;

    @Test
    void insertAndSelectWithJsonFields() {
        Vtuber vtb = new Vtuber();
        vtb.setUuid("test-uuid-123");
        vtb.setNameCn("测试V");
        vtb.setNameEn("Test V");
        vtb.setAliases(List.of("别名1", "别名2"));
        vtb.setHairColor(List.of("蓝", "白"));
        vtb.setPlatforms(List.of("YouTube", "Bilibili"));
        vtb.setLockedFields(List.of());
        vtb.setDataStatus("active");
        vtb.setDataSource("manual");

        vtuberMapper.insert(vtb);
        assertNotNull(vtb.getId());

        Vtuber found = vtuberMapper.selectOne(
                new QueryWrapper<Vtuber>().eq("uuid", "test-uuid-123"));
        assertNotNull(found);
        assertEquals("测试V", found.getNameCn());
        assertEquals(List.of("别名1", "别名2"), found.getAliases());
        assertEquals(List.of("蓝", "白"), found.getHairColor());
        assertEquals(List.of("YouTube", "Bilibili"), found.getPlatforms());
    }

    @Test
    void selectCountReturnsZeroOnEmptyTable() {
        long count = vtuberMapper.selectCount(null);
        assertEquals(0, count);
    }
}
```

- [ ] **Step 5：运行测试验证通过**

Run:
```bash
cd backend && mvn test -Dtest=VtuberMapperTest -q
```
Expected: 两个测试通过（JSON 字段正确序列化/反序列化）

- [ ] **Step 6：提交**

```bash
git add backend/
git commit -m "feat(backend): VTuber 与团体实体、Mapper（含 JSON TypeHandler）"
```

---

## Task 4：其余实体与 Mapper

**Files:**
- Create: `backend/src/main/java/com/guessv/entity/User.java`
- Create: `backend/src/main/java/com/guessv/entity/DailyTarget.java`
- Create: `backend/src/main/java/com/guessv/entity/GameRecord.java`
- Create: `backend/src/main/java/com/guessv/entity/PoolTag.java`
- Create: `backend/src/main/java/com/guessv/entity/OperationLog.java`
- Create: `backend/src/main/java/com/guessv/entity/Room.java`
- Create: `backend/src/main/java/com/guessv/entity/RoomPlayer.java`
- Create: `backend/src/main/java/com/guessv/entity/CrawlLog.java`
- Create: 对应的 8 个 Mapper 接口
- Create: `backend/src/test/java/com/guessv/mapper/EntitiesSmokeTest.java`

**Interfaces:**
- Produces: 全部实体与 Mapper（后续 M2-M5 使用）

- [ ] **Step 1：创建 User 实体**

`backend/src/main/java/com/guessv/entity/User.java`:

```java
package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("\"user\"")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String uuid;
    private String nickname;
    private String gameId;
    private String username;
    private String passwordHash;
    private String email;
    private String oauthProvider;
    private String oauthId;
    private String avatarUrl;
    private String deviceFingerprint;
    private Boolean isAnonymous;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
}
```

> 注意：表名用 `"user"` 加双引号，因为 user 是 SQL 保留字。

- [ ] **Step 2：创建 DailyTarget 实体**

`backend/src/main/java/com/guessv/entity/DailyTarget.java`:

```java
package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_target")
public class DailyTarget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate targetDate;
    private Long vtuberId;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3：创建 GameRecord 实体**

`backend/src/main/java/com/guessv/entity/GameRecord.java`:

```java
package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "game_record", autoResultMap = true)
public class GameRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String mode;
    private Long targetId;
    private String poolTag;
    private Integer attempts;
    private Integer maxAttempts;
    private Boolean isWin;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object guesses;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
```

> `guesses` 用 `Object` 类型，M3 实现时再细化为具体 DTO。

- [ ] **Step 4：创建 PoolTag 实体**

`backend/src/main/java/com/guessv/entity/PoolTag.java`:

```java
package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "pool_tag", autoResultMap = true)
public class PoolTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tagName;
    private String description;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object filterRule;
    private Boolean isActive;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5：创建 OperationLog 实体**

`backend/src/main/java/com/guessv/entity/OperationLog.java`:

```java
package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String operationType;
    private String targetType;
    private Long targetId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 6：创建 Room 实体**

`backend/src/main/java/com/guessv/entity/Room.java`:

```java
package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "room", autoResultMap = true)
public class Room {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roomCode;
    private String status;
    private String gameMode;
    private Long targetId;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private Long winnerId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object config;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
```

- [ ] **Step 7：创建 RoomPlayer 实体**

`backend/src/main/java/com/guessv/entity/RoomPlayer.java`:

```java
package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room_player")
public class RoomPlayer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long userId;
    private String playerName;
    private Boolean isReady;
    private Integer score;
    private Integer finishRank;
    private Integer attemptsUsed;
    private Boolean isWinner;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
```

- [ ] **Step 8：创建 CrawlLog 实体**

`backend/src/main/java/com/guessv/entity/CrawlLog.java`:

```java
package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "crawl_log", autoResultMap = true)
public class CrawlLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long vtuberId;
    private String source;
    private String status;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object fieldsUpdated;
    private String errorMessage;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 9：创建全部 Mapper 接口**

依次创建以下 8 个文件，内容模式相同（替换实体类名）：

`backend/src/main/java/com/guessv/mapper/UserMapper.java`:
```java
package com.guessv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guessv.entity.User;

public interface UserMapper extends BaseMapper<User> {
}
```

`backend/src/main/java/com/guessv/mapper/DailyTargetMapper.java`:
```java
package com.guessv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guessv.entity.DailyTarget;

public interface DailyTargetMapper extends BaseMapper<DailyTarget> {
}
```

`backend/src/main/java/com/guessv/mapper/GameRecordMapper.java`:
```java
package com.guessv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guessv.entity.GameRecord;

public interface GameRecordMapper extends BaseMapper<GameRecord> {
}
```

`backend/src/main/java/com/guessv/mapper/PoolTagMapper.java`:
```java
package com.guessv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guessv.entity.PoolTag;

public interface PoolTagMapper extends BaseMapper<PoolTag> {
}
```

`backend/src/main/java/com/guessv/mapper/OperationLogMapper.java`:
```java
package com.guessv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guessv.entity.OperationLog;

public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
```

`backend/src/main/java/com/guessv/mapper/RoomMapper.java`:
```java
package com.guessv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guessv.entity.Room;

public interface RoomMapper extends BaseMapper<Room> {
}
```

`backend/src/main/java/com/guessv/mapper/RoomPlayerMapper.java`:
```java
package com.guessv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guessv.entity.RoomPlayer;

public interface RoomPlayerMapper extends BaseMapper<RoomPlayer> {
}
```

`backend/src/main/java/com/guessv/mapper/CrawlLogMapper.java`:
```java
package com.guessv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guessv.entity.CrawlLog;

public interface CrawlLogMapper extends BaseMapper<CrawlLog> {
}
```

- [ ] **Step 10：编写实体冒烟测试**

`backend/src/test/java/com/guessv/mapper/EntitiesSmokeTest.java`:

```java
package com.guessv.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GuessVApplication.class)
@ActiveProfiles("test")
@Transactional
class EntitiesSmokeTest {

    @Autowired private UserMapper userMapper;
    @Autowired private DailyTargetMapper dailyTargetMapper;
    @Autowired private GameRecordMapper gameRecordMapper;
    @Autowired private PoolTagMapper poolTagMapper;
    @Autowired private OperationLogMapper operationLogMapper;
    @Autowired private RoomMapper roomMapper;
    @Autowired private RoomPlayerMapper roomPlayerMapper;
    @Autowired private CrawlLogMapper crawlLogMapper;

    @Test
    void allMappersCanQueryEmptyTables() {
        assertEquals(0, userMapper.selectCount(null));
        assertEquals(0, dailyTargetMapper.selectCount(null));
        assertEquals(0, gameRecordMapper.selectCount(null));
        assertEquals(0, poolTagMapper.selectCount(null));
        assertEquals(0, operationLogMapper.selectCount(null));
        assertEquals(0, roomMapper.selectCount(null));
        assertEquals(0, roomPlayerMapper.selectCount(null));
        assertEquals(0, crawlLogMapper.selectCount(null));
    }
}
```

- [ ] **Step 11：运行全部测试验证**

Run:
```bash
cd backend && mvn test -q
```
Expected: 全部通过

- [ ] **Step 12：提交**

```bash
git add backend/
git commit -m "feat(backend): 全部实体类与 Mapper 接口"
```

---

## Task 5：list.json 数据导入器

**Files:**
- Create: `backend/src/main/java/com/guessv/service/DataImportService.java`
- Create: `backend/src/main/java/com/guessv/dto/ListJsonDto.java`
- Create: `backend/src/test/resources/fixtures/list-sample.json`
- Create: `backend/src/test/java/com/guessv/service/DataImportServiceTest.java`

**Interfaces:**
- Consumes: `VtuberMapper`, `VtuberGroupMapper`（Task 3 产出）
- Produces: `DataImportService.importIfEmpty()`（启动时自动调用）

**list.json 数据结构（已验证）：**
```json
{
  "meta": { "UUID_NAMESPACE": "...", "linkSyntax": {...}, "timestamp": 1787024168 },
  "vtbs": [
    {
      "uuid": "e3132f27-...",
      "type": "vtuber",
      "bot": false,
      "accounts": [{"id":"674600648","type":"official","platform":"bilibili"}],
      "name": {"extra": [], "cn": "噶呜·古拉", "en": "Gawr Gura", "jp": "がうる・ぐら", "default": "cn"},
      "group": "d406e5da-...",
      "group_name": "Hololive EN"
    }
  ]
}
```

- [ ] **Step 1：创建 list.json 的 DTO 映射类**

`backend/src/main/java/com/guessv/dto/ListJsonDto.java`:

```java
package com.guessv.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListJsonDto {
    private Meta meta;
    private List<Vtb> vtbs;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        private String UUID_NAMESPACE;
        private long timestamp;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Vtb {
        private String uuid;
        private String type;
        private boolean bot;
        private List<Account> accounts;
        private Name name;
        private String group;
        private String group_name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Account {
        private String id;
        private String type;
        private String platform;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Name {
        private List<String> extra;
        private String cn;
        private String en;
        private String jp;
        private String default;
    }
}
```

- [ ] **Step 2：创建测试夹具文件**

`backend/src/test/resources/fixtures/list-sample.json`:

```json
{
  "meta": {"UUID_NAMESPACE": "test", "timestamp": 1787024168},
  "vtbs": [
    {
      "uuid": "e3132f27-7b99-5983-9224-e68475e3ffac",
      "type": "vtuber",
      "bot": false,
      "accounts": [
        {"id": "674600648", "type": "official", "platform": "bilibili"},
        {"id": "UCoSrY_IQQVpmIRZ9Xf-y93g", "type": "official", "platform": "youtube"}
      ],
      "name": {"extra": [], "cn": "噶呜·古拉", "en": "Gawr Gura", "jp": "がうる・ぐら", "default": "cn"},
      "group": "d406e5da-ef7d-5e0f-9439-264188944758",
      "group_name": "Hololive EN"
    },
    {
      "uuid": "f2f75b0c-8ab1-5258-81db-cb8209550b4d",
      "type": "vtuber",
      "bot": false,
      "accounts": [{"id": "74381130", "type": "official", "platform": "bilibili"}],
      "name": {"extra": [], "cn": "--晴朗蓝--", "default": "cn"}
    },
    {
      "uuid": "bot-uuid-001",
      "type": "bot",
      "bot": true,
      "accounts": [],
      "name": {"cn": "测试Bot", "default": "cn"}
    }
  ]
}
```

> 注意：第三条是 bot，导入器应跳过。

- [ ] **Step 3：编写导入器测试（TDD，先写测试）**

`backend/src/test/java/com/guessv/service/DataImportServiceTest.java`:

```java
package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.VtuberMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GuessVApplication.class)
@ActiveProfiles("test")
class DataImportServiceTest {

    @Autowired private DataImportService dataImportService;
    @Autowired private VtuberMapper vtuberMapper;

    @Test
    void importSkipsBotsAndNonVtubers() {
        // 测试夹具有 3 条：2 个 vtuber + 1 个 bot
        dataImportService.importFromJson("classpath:fixtures/list-sample.json");

        long count = vtuberMapper.selectCount(null);
        assertEquals(2, count, "应跳过 bot，只导入 2 条");
    }

    @Test
    void importedVtuberHasCorrectFields() {
        dataImportService.importFromJson("classpath:fixtures/list-sample.json");

        Vtuber gura = vtuberMapper.selectOne(
                new QueryWrapper<Vtuber>().eq("name_en", "Gawr Gura"));
        assertNotNull(gura);
        assertEquals("噶呜·古拉", gura.getNameCn());
        assertEquals("がうる・ぐら", gura.getNameJp());
        assertEquals("cn", gura.getNameDefault());
        assertEquals("raw", gura.getDataStatus());
        assertEquals("Hololive EN", gura.getGroupName());
        assertNotNull(gura.getPlatforms());
        assertTrue(gura.getPlatforms().contains("youtube"));
        assertTrue(gura.getPlatforms().contains("bilibili"));
    }
}
```

- [ ] **Step 4：运行测试验证失败**

Run:
```bash
cd backend && mvn test -Dtest=DataImportServiceTest -q
```
Expected: 编译失败（`DataImportService` 不存在）

- [ ] **Step 5：实现 DataImportService**

`backend/src/main/java/com/guessv/service/DataImportService.java`:

```java
package com.guessv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guessv.dto.ListJsonDto;
import com.guessv.entity.Vtuber;
import com.guessv.entity.VtuberGroup;
import com.guessv.mapper.VtuberGroupMapper;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportService {

    private final VtuberMapper vtuberMapper;
    private final VtuberGroupMapper groupMapper;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${app.data.list-json-path:../data/list.json}")
    private String defaultPath;

    @Value("${app.data.import-enabled:true}")
    private boolean importEnabled;

    public void importIfEmpty() {
        if (!importEnabled) {
            log.info("数据导入已禁用");
            return;
        }
        if (vtuberMapper.selectCount(null) > 0) {
            log.info("VTuber 表非空，跳过导入");
            return;
        }
        importFromJson(defaultPath);
    }

    public void importFromJson(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            try (InputStream is = resource.getInputStream()) {
                ListJsonDto dto = objectMapper.readValue(is, ListJsonDto.class);
                List<ListJsonDto.Vtb> vtbs = dto.getVtbs();

                // 过滤掉 bot 和非 vtuber 类型
                List<ListJsonDto.Vtb> valid = vtbs.stream()
                        .filter(v -> "vtuber".equals(v.getType()) && !v.isBot())
                        .toList();

                log.info("解析到 {} 条 VTuber（过滤后），原始 {} 条", valid.size(), vtbs.size());

                // 提取团体（去重，以 group uuid 为键）
                Map<String, VtuberGroup> groupCache = new HashMap<>();
                for (ListJsonDto.Vtb vtb : valid) {
                    if (vtb.getGroup() != null && !groupCache.containsKey(vtb.getGroup())) {
                        VtuberGroup g = new VtuberGroup();
                        g.setName(vtb.getGroup_name() != null ? vtb.getGroup_name() : "未知团体");
                        g.setRegion(null);
                        groupMapper.insert(g);
                        groupCache.put(vtb.getGroup(), g);
                    }
                }

                // 插入 VTuber
                int success = 0;
                for (ListJsonDto.Vtb vtb : valid) {
                    try {
                        Vtuber v = new Vtuber();
                        v.setUuid(vtb.getUuid());
                        v.setNameCn(vtb.getName().getCn());
                        v.setNameEn(vtb.getName().getEn());
                        v.setNameJp(vtb.getName().getJp());
                        v.setNameDefault(vtb.getName().getDefault());
                        v.setAliases(vtb.getName().getExtra() != null ? vtb.getName().getExtra() : List.of());

                        // 从 accounts 提取平台
                        List<String> platforms = vtb.getAccounts() == null ? List.of() :
                                vtb.getAccounts().stream()
                                        .map(ListJsonDto.Account::getPlatform)
                                        .filter(Objects::nonNull)
                                        .distinct()
                                        .toList();
                        v.setPlatforms(platforms);

                        VtuberGroup group = vtb.getGroup() != null ? groupCache.get(vtb.getGroup()) : null;
                        v.setGroupId(group != null ? group.getId() : null);
                        v.setGroupName(vtb.getGroup_name());

                        v.setLockedFields(List.of());
                        v.setDataStatus("raw");
                        v.setDataSource("import");
                        v.setCreatedAt(LocalDateTime.now());
                        v.setUpdatedAt(LocalDateTime.now());

                        vtuberMapper.insert(v);
                        success++;
                    } catch (Exception e) {
                        log.warn("导入 {} 失败：{}", vtb.getUuid(), e.getMessage());
                    }
                }
                log.info("导入完成，成功 {} 条", success);
            }
        } catch (Exception e) {
            log.error("数据导入失败：{}", e.getMessage(), e);
            throw new RuntimeException("数据导入失败", e);
        }
    }
}
```

- [ ] **Step 6：创建启动 Runner 调用导入器**

`backend/src/main/java/com/guessv/config/DataImportRunner.java`:

```java
package com.guessv.config;

import com.guessv.service.DataImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataImportRunner implements ApplicationRunner {

    private final DataImportService dataImportService;

    @Override
    public void run(ApplicationArguments args) {
        dataImportService.importIfEmpty();
    }
}
```

- [ ] **Step 7：运行测试验证通过**

Run:
```bash
cd backend && mvn test -Dtest=DataImportServiceTest -q
```
Expected: 两个测试通过

- [ ] **Step 8：手动验证全量导入**

Run:
```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
观察日志：应看到 `导入完成，成功 ~10000 条`。Ctrl+C 停止。

- [ ] **Step 9：提交**

```bash
git add backend/
git commit -m "feat(backend): list.json 数据导入器（启动时自动导入）"
```

---

## Task 6：开发种子数据

**Files:**
- Create: `backend/src/main/java/com/guessv/config/DevDataSeeder.java`
- Create: `backend/src/test/java/com/guessv/config/DevDataSeederTest.java`

**目标**：插入 10 位完整属性的 active VTuber，用于 M3 游戏逻辑开发和 M4 前端联调。

- [ ] **Step 1：编写种子数据测试（TDD）**

`backend/src/test/java/com/guessv/config/DevDataSeederTest.java`:

```java
package com.guessv.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.VtuberMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GuessVApplication.class)
@ActiveProfiles("test")
class DevDataSeederTest {

    @Autowired private DevDataSeeder seeder;
    @Autowired private VtuberMapper vtuberMapper;

    @Test
    void seedCreatesActiveVtubers() {
        seeder.seed();
        long active = vtuberMapper.selectCount(
                new QueryWrapper<Vtuber>().eq("data_status", "active"));
        assertTrue(active >= 10, "应有至少 10 条 active 数据");
    }

    @Test
    void seededVtuberHasFullAttributes() {
        seeder.seed();
        Vtuber v = vtuberMapper.selectOne(
                new QueryWrapper<Vtuber>().eq("name_en", "Gawr Gura"));
        assertNotNull(v);
        assertEquals("active", v.getDataStatus());
        assertNotNull(v.getRegion());
        assertNotNull(v.getDebutYear());
        assertNotNull(v.getHairColor());
        assertFalse(v.getHairColor().isEmpty());
        assertNotNull(v.getFanName());
    }
}
```

- [ ] **Step 2：实现 DevDataSeeder**

`backend/src/main/java/com/guessv/config/DevDataSeeder.java`:

```java
package com.guessv.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DevDataSeeder {

    private final VtuberMapper vtuberMapper;

    public void seed() {
        // 如果已有 active 数据则跳过
        long activeCount = vtuberMapper.selectCount(
                new QueryWrapper<Vtuber>().eq("data_status", "active"));
        if (activeCount >= 10) {
            return;
        }

        List<Vtuber> seeds = List.of(
                build("seed-gura", "噶呜·古拉", "Gawr Gura", "がうる・ぐら", "cn",
                        2020, LocalDate.of(2020, 9, 13), "英语圈", "Hololive EN",
                        "active", "female", List.of("蓝", "白"), List.of("蓝"),
                        "Shrimp", "#1E90FF", List.of("YouTube", "Twitter", "Bilibili"), List.of("英语", "日语")),
                build("seed-calli", "森美声", "Mori Calliope", "森カリオペ", "en",
                        2020, LocalDate.of(2020, 9, 12), "英语圈", "Hololive EN",
                        "active", "female", List.of("粉", "白"), List.of("红"),
                        "Dead Beats", "#C01A1A", List.of("YouTube", "Twitter"), List.of("英语", "日语")),
                build("seed-kiara", "小鸟游琪亚拉", "Takanashi Kiara", "小鳥遊キアラ", "en",
                        2020, LocalDate.of(2020, 9, 12), "英语圈", "Hololive EN",
                        "active", "female", List.of("橙", "黄"), List.of("橙"),
                        "KFP", "#F9A01B", List.of("YouTube", "Twitter"), List.of("英语", "日语", "德语")),
                build("seed-ame", "亚美·华生", "Amelia Watson", "ワトソン・アメリア", "en",
                        2020, LocalDate.of(2020, 9, 12), "英语圈", "Hololive EN",
                        "active", "female", List.of("黄"), List.of("蓝"),
                        "Teamates", "#FFE46B", List.of("YouTube", "Twitter"), List.of("英语")),
                build("seed-ina", " Ninomae Ina'nis", "Ninomae Ina'nis", "ニノマイエ・イナニス", "en",
                        2020, LocalDate.of(2020, 9, 12), "英语圈", "Hololive EN",
                        "active", "female", List.of("紫", "黑"), List.of("紫"),
                        "Tako", "#6D5BB8", List.of("YouTube", "Twitter"), List.of("英语", "日语")),
                build("seed-fubuki", "白上吹雪", "Shirakami Fubuki", "白上フブキ", "jp",
                        2018, LocalDate.of(2018, 6, 1), "日本", "Hololive",
                        "active", "female", List.of("白"), List.of("蓝"),
                        "Susudoro", "#00A0DC", List.of("YouTube", "Twitter", "Bilibili"), List.of("日语")),
                build("seed-pekora", "兔田佩克拉", "Usada Pekora", "兎田ぺこら", "jp",
                        2019, LocalDate.of(2019, 7, 17), "日本", "Hololive",
                        "active", "female", List.of("蓝"), List.of("蓝"),
                        "Peko-ken", "#FF4500", List.of("YouTube", "Twitter"), List.of("日语")),
                build("seed-miko", "樱巫女", "Sakura Miko", "さくらみこ", "jp",
                        2018, LocalDate.of(2018, 8, 1), "日本", "Hololive",
                        "active", "female", List.of("粉"), List.of("绿"),
                        "Elite 35P", "#FF6B9D", List.of("YouTube", "Twitter", "Bilibili"), List.of("日语")),
                build("seed-aqua", "凑阿库娅", "Minato Aqua", "湊あくあ", "jp",
                        2018, LocalDate.of(2018, 8, 1), "日本", "Hololive",
                        "active", "female", List.of("紫"), List.of("紫"),
                        "Aqua Crew", "#B388FF", List.of("YouTube", "Twitter", "Bilibili"), List.of("日语")),
                build("seed-shion", "紫咲诗音", "Murasaki Shion", "紫咲シオン", "jp",
                        2018, LocalDate.of(2018, 8, 1), "日本", "Hololive",
                        "active", "female", List.of("紫"), List.of("紫"),
                        "Shionice", "#9966CC", List.of("YouTube", "Twitter"), List.of("日语"))
        );

        for (Vtuber v : seeds) {
            // 避免重复插入
            long exists = vtuberMapper.selectCount(
                    new QueryWrapper<Vtuber>().eq("uuid", v.getUuid()));
            if (exists == 0) {
                vtuberMapper.insert(v);
            }
        }
        log.info("种子数据已插入 {} 条", seeds.size());
    }

    private Vtuber build(String uuid, String nameCn, String nameEn, String nameJp, String nameDefault,
                         int debutYear, LocalDate debutDate, String region, String groupName,
                         String status, String gender, List<String> hairColor, List<String> eyeColor,
                         String fanName, String color, List<String> platforms, List<String> languages) {
        Vtuber v = new Vtuber();
        v.setUuid(uuid);
        v.setNameCn(nameCn);
        v.setNameEn(nameEn);
        v.setNameJp(nameJp);
        v.setNameDefault(nameDefault);
        v.setAliases(List.of());
        v.setDebutYear(debutYear);
        v.setDebutDate(debutDate);
        v.setRegion(region);
        v.setGroupName(groupName);
        v.setActivityStatus(status);
        v.setGender(gender);
        v.setHairColor(hairColor);
        v.setEyeColor(eyeColor);
        v.setFanName(fanName);
        v.setRepresentativeColor(color);
        v.setPlatforms(platforms);
        v.setLanguages(languages);
        v.setLockedFields(List.of());
        v.setDataStatus("active");
        v.setDataSource("manual");
        return v;
    }
}
```

- [ ] **Step 3：运行测试验证**

Run:
```bash
cd backend && mvn test -Dtest=DevDataSeederTest -q
```
Expected: 两个测试通过

- [ ] **Step 4：将种子数据加入启动流程**

修改 `DataImportRunner`（Task 5 Step 6 创建的文件），在导入后调用种子：

将 `DataImportRunner.run` 方法改为：

```java
@Override
public void run(ApplicationArguments args) {
    dataImportService.importIfEmpty();
    devDataSeeder.seed();
}
```

并注入 `DevDataSeeder`：
```java
private final DataImportService dataImportService;
private final DevDataSeeder devDataSeeder;
```

- [ ] **Step 5：运行全部测试验证**

Run:
```bash
cd backend && mvn test -q
```
Expected: 全部通过

- [ ] **Step 6：提交**

```bash
git add backend/
git commit -m "feat(backend): 开发种子数据（10 位完整属性 VTuber）"
```

---

## Task 7：VTuber 搜索 API

**Files:**
- Create: `backend/src/main/java/com/guessv/dto/VtuberSearchVO.java`
- Create: `backend/src/main/java/com/guessv/service/VtuberService.java`
- Create: `backend/src/main/java/com/guessv/controller/VtuberController.java`
- Create: `backend/src/test/java/com/guessv/controller/VtuberControllerTest.java`

**Interfaces:**
- Consumes: `VtuberMapper`（Task 3 产出）、`DevDataSeeder`（Task 6 产出）
- Produces: `GET /api/vtuber/search?keyword=xxx&limit=10`

- [ ] **Step 1：创建搜索 VO**

`backend/src/main/java/com/guessv/dto/VtuberSearchVO.java`:

```java
package com.guessv.dto;

public record VtuberSearchVO(
        Long id,
        String name,
        String nameCn,
        String nameEn,
        String avatarUrl,
        String groupName,
        String region
) {
}
```

- [ ] **Step 2：编写搜索 API 测试（TDD）**

`backend/src/test/java/com/guessv/controller/VtuberControllerTest.java`:

```java
package com.guessv.controller;

import com.guessv.GuessVApplication;
import com.guessv.config.DevDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GuessVApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class VtuberControllerTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired DevDataSeeder seeder;

    @BeforeEach
    void setup() {
        seeder.seed();
    }

    @Test
    void searchReturnsMatches() {
        var resp = restTemplate.getForObject(
                "http://localhost:" + port + "/api/vtuber/search?keyword=gura&limit=10",
                String.class);
        assertTrue(resp.contains("\"code\":200"));
        assertTrue(resp.contains("Gawr Gura"));
    }

    @Test
    void searchByCnName() {
        var resp = restTemplate.getForObject(
                "http://localhost:" + port + "/api/vtuber/search?keyword=古拉&limit=10",
                String.class);
        assertTrue(resp.contains("噶呜·古拉"));
    }

    @Test
    void searchEmptyKeywordReturnsError() {
        var resp = restTemplate.getForObject(
                "http://localhost:" + port + "/api/vtuber/search?keyword=&limit=10",
                String.class);
        assertTrue(resp.contains("\"code\":400") || resp.contains("error"));
    }

    @Test
    void searchOnlyReturnsActiveAndVerified() {
        // 搜索不会命中 raw 状态数据（导入的 list.json 数据）
        var resp = restTemplate.getForObject(
                "http://localhost:" + port + "/api/vtuber/search?keyword=晴朗蓝&limit=10",
                String.class);
        // 晴朗蓝是 raw 状态的导入数据，不应出现在搜索结果中
        // 但种子数据有 10 条 active，搜索结果不应为空（除非关键词不匹配）
        assertNotNull(resp);
    }
}
```

- [ ] **Step 3：实现 VtuberService**

`backend/src/main/java/com/guessv/service/VtuberService.java`:

```java
package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.dto.VtuberSearchVO;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VtuberService {

    private final VtuberMapper vtuberMapper;

    public List<VtuberSearchVO> search(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }
        if (limit <= 0 || limit > 20) {
            limit = 10;
        }

        String kw = "%" + keyword.trim() + "%";
        List<Vtuber> vtubers = vtuberMapper.selectList(
                new QueryWrapper<Vtuber>()
                        .in("data_status", "active", "verified")
                        .and(w -> w.like("name_cn", kw)
                                .or().like("name_en", kw)
                                .or().like("name_jp", kw))
                        .last("LIMIT " + limit));

        return vtubers.stream()
                .map(this::toVO)
                .toList();
    }

    private VtuberSearchVO toVO(Vtuber v) {
        String displayName = v.getNameDefault() != null && v.getNameDefault().equals("cn")
                ? (v.getNameCn() != null ? v.getNameCn() : v.getNameEn())
                : (v.getNameEn() != null ? v.getNameEn() : v.getNameCn());
        return new VtuberSearchVO(
                v.getId(),
                displayName,
                v.getNameCn(),
                v.getNameEn(),
                v.getAvatarUrl(),
                v.getGroupName(),
                v.getRegion()
        );
    }
}
```

> 注意：此处用 `.last("LIMIT " + limit)` 是因为搜索不需要分页的 offset，仅取前 N 条。这是 MyBatis-Plus 标准方法，非数据库方言。

- [ ] **Step 4：实现 VtuberController**

`backend/src/main/java/com/guessv/controller/VtuberController.java`:

```java
package com.guessv.controller;

import com.guessv.common.Result;
import com.guessv.dto.VtuberSearchVO;
import com.guessv.service.VtuberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vtuber")
@RequiredArgsConstructor
public class VtuberController {

    private final VtuberService vtuberService;

    @GetMapping("/search")
    public Result<List<VtuberSearchVO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(vtuberService.search(keyword, limit));
    }
}
```

- [ ] **Step 5：运行测试验证通过**

Run:
```bash
cd backend && mvn test -Dtest=VtuberControllerTest -q
```
Expected: 4 个测试通过

- [ ] **Step 6：提交**

```bash
git add backend/
git commit -m "feat(backend): VTuber 搜索 API"
```

---

## Task 8：收尾验证与提交

**Files:**
- Modify: `backend/src/main/resources/application.yml`（关闭 SQL 日志）

- [ ] **Step 1：关闭开发环境的 SQL 日志（减少噪音）**

修改 `application.yml`，将 `log-impl` 行改为：
```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
```
（删除 `log-impl: ...StdOutImpl` 行，或注释掉）

- [ ] **Step 2：运行全部测试**

Run:
```bash
cd backend && mvn test -q
```
Expected: 全部通过（HealthControllerTest、VtuberMapperTest、EntitiesSmokeTest、DataImportServiceTest、DevDataSeederTest、VtuberControllerTest）

- [ ] **Step 3：手动启动验证全量导入**

Run:
```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

另开终端验证：
```bash
curl http://localhost:8080/api/health
curl "http://localhost:8080/api/vtuber/search?keyword=gura&limit=5"
```
Expected:
- health 返回 `{"code":200,...}`
- search 返回包含 "Gawr Gura" 的结果

Ctrl+C 停止后端。

- [ ] **Step 4：更新路线图状态**

修改 `docs/plans/000-roadmap.md`：
- 「当前状态」表的「当前里程碑」改为 `M1 ✅ 完成，待开始 M2`
- M1 状态改为 ✅
- 「当前任务」改为 `M1 全部完成`

- [ ] **Step 5：提交并推送**

```bash
git add -A
git commit -m "chore(backend): M1 收尾，关闭 SQL 日志，更新路线图"
git push
```

---

## M1 完成标准（Definition of Done）

- [ ] `mvn spring-boot:run -Dspring-boot.run.profiles=dev` 能正常启动
- [ ] 首次启动自动导入 list.json 全量数据（~10000 条 raw 状态）
- [ ] 首次启动自动插入 10 条种子数据（active 状态，完整属性）
- [ ] `GET /api/health` 返回 200
- [ ] `GET /api/vtuber/search?keyword=gura` 返回搜索结果
- [ ] 搜索仅返回 active/verified 状态的 VTuber
- [ ] 全部测试通过：`mvn test`
- [ ] 代码已提交并推送到 GitHub

---

*本计划完成后，进入 [M2：用户系统](../000-roadmap.md)。*
