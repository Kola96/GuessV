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
