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
