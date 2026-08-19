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
        var a = userService.createAnonymousUser("甲甲", "fp1");
        var b = userService.createAnonymousUser("乙乙", "fp2");
        assertNotEquals(a.gameId(), b.gameId());
    }
}
