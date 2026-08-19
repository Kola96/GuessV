package com.guessv.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.VtuberMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GuessVApplication.class)
@ActiveProfiles("test")
@Transactional
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
