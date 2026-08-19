package com.guessv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.GuessVApplication;
import com.guessv.config.DevDataSeeder;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.VtuberMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 注意：本测试通过 TestRestTemplate 发真实 HTTP 请求，服务端在独立线程处理，
 * 因此不能用 @Transactional 回滚做隔离（服务端看不到测试线程的未提交数据）。
 * 改为 @BeforeEach 播种 + @AfterEach 清理，保持共享测试库干净。
 */
@SpringBootTest(classes = GuessVApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class VtuberControllerTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired DevDataSeeder seeder;
    @Autowired VtuberMapper vtuberMapper;

    @BeforeEach
    void setup() {
        seeder.seed();
    }

    @AfterEach
    void cleanup() {
        vtuberMapper.delete(new QueryWrapper<Vtuber>().eq("data_source", "manual"));
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void searchReturnsMatches() {
        var resp = restTemplate.getForObject(
                url("/api/vtuber/search?keyword=gura&limit=10"), String.class);
        assertTrue(resp.contains("\"code\":200"));
        assertTrue(resp.contains("Gawr Gura"));
    }

    @Test
    void searchByCnName() {
        var resp = restTemplate.getForObject(
                url("/api/vtuber/search?keyword=古拉&limit=10"), String.class);
        assertTrue(resp.contains("噶呜·古拉"));
    }

    @Test
    void searchByJpName() {
        var resp = restTemplate.getForObject(
                url("/api/vtuber/search?keyword=みこ&limit=10"), String.class);
        assertTrue(resp.contains("Sakura Miko"));
    }

    @Test
    void searchEmptyKeywordReturns400() {
        var resp = restTemplate.getForObject(
                url("/api/vtuber/search?keyword=&limit=10"), String.class);
        assertTrue(resp.contains("\"code\":400"));
    }

    @Test
    void searchOnlyReturnsActiveAndVerified() {
        // raw 状态的导入数据不应出现在搜索结果中。
        // 先用导入器夹具数据不可用（test 环境 import-enabled=false），
        // 手动插入一条 raw 数据验证被过滤
        Vtuber raw = new Vtuber();
        raw.setUuid("raw-test-uuid");
        raw.setNameCn("原始数据V");
        raw.setDataStatus("raw");
        raw.setDataSource("import");
        vtuberMapper.insert(raw);

        var resp = restTemplate.getForObject(
                url("/api/vtuber/search?keyword=原始数据&limit=10"), String.class);
        assertTrue(resp.contains("\"code\":200"));
        assertFalse(resp.contains("原始数据V"));

        vtuberMapper.delete(new QueryWrapper<Vtuber>().eq("uuid", "raw-test-uuid"));
    }
}
