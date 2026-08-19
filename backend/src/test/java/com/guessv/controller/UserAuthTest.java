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

    private HttpHeaders authHeader(String token) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) h.set("X-User-Token", token);
        return h;
    }

    @Test
    void initEndpointIsPublic() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(
                "{\"nickname\":\"测试用户\",\"deviceFingerprint\":\"fp\"}", h);
        var resp = restTemplate.postForEntity(url("/api/user/init"), req, String.class);
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("测试用户#"));
    }

    @Test
    void profileRejectsMissingToken() {
        var resp = restTemplate.getForEntity(url("/api/user/profile"), String.class);
        assertTrue(resp.getBody().contains("\"code\":401"));
    }

    @Test
    void profileRejectsInvalidToken() {
        var entity = new HttpEntity<>(authHeader("invalid.token.here"));
        var resp = restTemplate.exchange(url("/api/user/profile"), HttpMethod.GET, entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":401"));
    }

    @Test
    void profileAcceptsValidToken() {
        var init = userService.createAnonymousUser("鉴权测试", "fp");
        var entity = new HttpEntity<>(authHeader(init.token()));
        var resp = restTemplate.exchange(url("/api/user/profile"), HttpMethod.GET, entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("鉴权测试"));
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
        HttpHeaders h = authHeader(init.token());
        h.setContentType(MediaType.APPLICATION_JSON);
        var entity = new HttpEntity<>("{\"nickname\":\"新昵称\"}", h);
        var resp = restTemplate.exchange(url("/api/user/nickname"), HttpMethod.PUT, entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":200"));
        assertTrue(resp.getBody().contains("新昵称"));
    }

    @Test
    void changeNicknameRejectsSensitive() {
        var init = userService.createAnonymousUser("合法昵称", "fp");
        HttpHeaders h = authHeader(init.token());
        h.setContentType(MediaType.APPLICATION_JSON);
        var entity = new HttpEntity<>("{\"nickname\":\"色情主播\"}", h);
        var resp = restTemplate.exchange(url("/api/user/nickname"), HttpMethod.PUT, entity, String.class);
        assertTrue(resp.getBody().contains("\"code\":400"));
    }
}
