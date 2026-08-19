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
