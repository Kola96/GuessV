package com.guessv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.config.DevDataSeeder;
import com.guessv.entity.DailyTarget;
import com.guessv.entity.GameRecord;
import com.guessv.entity.User;
import com.guessv.mapper.DailyTargetMapper;
import com.guessv.mapper.GameRecordMapper;
import com.guessv.mapper.UserMapper;
import com.guessv.mapper.VtuberMapper;
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
    @Autowired VtuberMapper vtuberMapper;
    @Autowired DailyTargetMapper dailyTargetMapper;
    @Autowired GameRecordMapper gameRecordMapper;
    @Autowired DevDataSeeder seeder;
    @Autowired TestPoolHelper testPoolHelper;
    private String token;

    @BeforeEach
    void setup() {
        seeder.seed();
        testPoolHelper.setupTestPools();
        token = userService.createAnonymousUser("玩家", "fp").token();
    }

    @AfterEach
    void cleanup() {
        gameRecordMapper.delete(new QueryWrapper<GameRecord>().eq("mode", "daily"));
        dailyTargetMapper.delete(new QueryWrapper<>());
        userMapper.delete(new QueryWrapper<User>().eq("is_anonymous", true));
        vtuberMapper.delete(new QueryWrapper<com.guessv.entity.Vtuber>().eq("data_source", "manual"));
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
    void guessReturnsComparison() {
        var seed = vtuberMapper.selectOne(new QueryWrapper<com.guessv.entity.Vtuber>()
                .eq("data_source", "manual").last("LIMIT 1"));
        assertNotNull(seed, "应有种子数据");
        HttpEntity<String> req = new HttpEntity<>(
                "{\"vtuberId\":" + seed.getId() + "}", auth());
        var resp = restTemplate.postForEntity(url("/api/game/daily/guess"), req, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("comparison"));
    }

    @Test
    void duplicateGuessRejected() {
        var seed = vtuberMapper.selectOne(new QueryWrapper<com.guessv.entity.Vtuber>()
                .eq("data_source", "manual").last("LIMIT 1"));
        assertNotNull(seed, "应有种子数据");
        HttpEntity<String> req = new HttpEntity<>(
                "{\"vtuberId\":" + seed.getId() + "}", auth());
        restTemplate.postForEntity(url("/api/game/daily/guess"), req, String.class);
        var resp2 = restTemplate.postForEntity(url("/api/game/daily/guess"), req, String.class);
        assertTrue(resp2.getBody().contains("409") || resp2.getBody().contains("猜过"));
    }
}
