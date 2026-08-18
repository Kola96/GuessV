package com.guessv.service;

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
