package com.guessv.mapper;

import com.guessv.GuessVApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GuessVApplication.class)
@ActiveProfiles("test")
@Transactional
class EntitiesSmokeTest {

    @Autowired private UserMapper userMapper;
    @Autowired private DailyTargetMapper dailyTargetMapper;
    @Autowired private GameRecordMapper gameRecordMapper;
    @Autowired private PoolTagMapper poolTagMapper;
    @Autowired private OperationLogMapper operationLogMapper;
    @Autowired private RoomMapper roomMapper;
    @Autowired private RoomPlayerMapper roomPlayerMapper;
    @Autowired private CrawlLogMapper crawlLogMapper;

    @Test
    void allMappersCanQueryEmptyTables() {
        assertEquals(0, userMapper.selectCount(null));
        assertEquals(0, dailyTargetMapper.selectCount(null));
        assertEquals(0, gameRecordMapper.selectCount(null));
        assertEquals(0, poolTagMapper.selectCount(null));
        assertEquals(0, operationLogMapper.selectCount(null));
        assertEquals(0, roomMapper.selectCount(null));
        assertEquals(0, roomPlayerMapper.selectCount(null));
        assertEquals(0, crawlLogMapper.selectCount(null));
    }
}
