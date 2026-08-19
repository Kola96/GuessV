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
    void allMappersCanQueryWithoutError() {
        // 仅验证各 Mapper 能正常查询（不要求表为空，因 Controller 测试会提交种子数据到共享 test.db）
        assertDoesNotThrow(() -> userMapper.selectCount(null));
        assertDoesNotThrow(() -> dailyTargetMapper.selectCount(null));
        assertDoesNotThrow(() -> gameRecordMapper.selectCount(null));
        assertDoesNotThrow(() -> poolTagMapper.selectCount(null));
        assertDoesNotThrow(() -> operationLogMapper.selectCount(null));
        assertDoesNotThrow(() -> roomMapper.selectCount(null));
        assertDoesNotThrow(() -> roomPlayerMapper.selectCount(null));
        assertDoesNotThrow(() -> crawlLogMapper.selectCount(null));
    }
}
