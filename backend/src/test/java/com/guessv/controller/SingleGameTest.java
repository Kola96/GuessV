package com.guessv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.config.DevDataSeeder;
import com.guessv.entity.GameRecord;
import com.guessv.entity.User;
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
class SingleGameTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired UserService userService;
    @Autowired UserMapper userMapper;
    @Autowired VtuberMapper vtuberMapper;
    @Autowired GameRecordMapper gameRecordMapper;
    @Autowired DevDataSeeder seeder;
    @Autowired TestPoolHelper testPoolHelper;
    private String token;

    @BeforeEach
    void setup() {
        seeder.seed();
        testPoolHelper.setupTestPools();
        token = userService.createAnonymousUser("单人玩家", "fp").token();
    }

    @AfterEach
    void cleanup() {
        gameRecordMapper.delete(new QueryWrapper<GameRecord>().eq("mode", "single"));
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
    void listPoolsReturnsNonEmpty() {
        var entity = new HttpEntity<>(auth());
        var resp = restTemplate.exchange(url("/api/game/single/pools"), HttpMethod.GET, entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("test-single"));
    }

    @Test
    void startReturnsSessionId() {
        var entity = new HttpEntity<>("{\"poolTag\":\"test-single\"}", auth());
        var resp = restTemplate.postForEntity(url("/api/game/single/start"), entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("sessionId"));
    }

    @Test
    void guessOnSingleSessionWorks() {
        // 查询一个真实的种子 VTuber id
        var seed = vtuberMapper.selectOne(new QueryWrapper<com.guessv.entity.Vtuber>()
                .eq("data_source", "manual").last("LIMIT 1"));
        assertNotNull(seed, "应有种子数据");

        var startEntity = new HttpEntity<>("{\"poolTag\":\"test-single\"}", auth());
        var startResp = restTemplate.postForEntity(url("/api/game/single/start"), startEntity, String.class);
        String body = startResp.getBody();
        int idx = body.indexOf("\"sessionId\":");
        int valStart = idx + 12;  // "sessionId": 是 12 字符
        int valEnd = body.indexOf(',', valStart);
        if (valEnd < 0) valEnd = body.indexOf('}', valStart);
        long sessionId = Long.parseLong(body.substring(valStart, valEnd).trim());

        var guessEntity = new HttpEntity<>(
                "{\"sessionId\":" + sessionId + ",\"vtuberId\":" + seed.getId() + "}", auth());
        var resp = restTemplate.postForEntity(url("/api/game/single/guess"), guessEntity, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("comparison"));
    }
}
