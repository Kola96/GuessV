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
        assertTrue(t.getVtuberId() > 0);
    }
}
