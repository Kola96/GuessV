package com.guessv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.entity.Pool;
import com.guessv.entity.PoolItem;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.PoolItemMapper;
import com.guessv.mapper.PoolMapper;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 测试专用题库初始化辅助。
 * 在测试的 @BeforeEach 中调用，创建每日+单人题库并加入种子 V。
 */
@Component
@RequiredArgsConstructor
public class TestPoolHelper {

    private final PoolMapper poolMapper;
    private final PoolItemMapper poolItemMapper;
    private final VtuberMapper vtuberMapper;

    public void setupTestPools() {
        // 清理旧题库
        poolItemMapper.delete(new QueryWrapper<>());
        poolMapper.delete(new QueryWrapper<>());

        // 查种子 V
        List<Vtuber> seeds = vtuberMapper.selectList(
                new QueryWrapper<Vtuber>().eq("data_source", "manual")
                        .in("market", "cn", "both"));
        if (seeds.isEmpty()) return;

        // 创建每日题库
        Pool dailyPool = new Pool();
        dailyPool.setName("test-daily");
        dailyPool.setMarket("cn");
        dailyPool.setMode("daily");
        dailyPool.setIsActive(true);
        poolMapper.insert(dailyPool);

        // 创建单人题库
        Pool singlePool = new Pool();
        singlePool.setName("test-single");
        singlePool.setMarket("cn");
        singlePool.setMode("single");
        singlePool.setIsActive(true);
        poolMapper.insert(singlePool);

        for (Vtuber v : seeds) {
            PoolItem di = new PoolItem();
            di.setPoolId(dailyPool.getId());
            di.setVtuberId(v.getId());
            poolItemMapper.insert(di);

            PoolItem si = new PoolItem();
            si.setPoolId(singlePool.getId());
            si.setVtuberId(v.getId());
            poolItemMapper.insert(si);
        }
    }
}
