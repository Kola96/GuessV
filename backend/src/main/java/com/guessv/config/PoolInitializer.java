package com.guessv.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.entity.Pool;
import com.guessv.entity.PoolItem;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.PoolItemMapper;
import com.guessv.mapper.PoolMapper;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 初始化默认题库：启动时如果没有题库，自动创建每日题库和单人题库，
 * 把所有 active/verified 的 cn/both V 加入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PoolInitializer {

    private final PoolMapper poolMapper;
    private final PoolItemMapper poolItemMapper;
    private final VtuberMapper vtuberMapper;

    @Value("${app.data.seed-enabled:true}")
    private boolean seedEnabled;

    public void initIfEmpty() {
        if (!seedEnabled) return;
        long count = poolMapper.selectCount(null);
        if (count > 0) return;

        log.info("题库为空，初始化默认题库...");

        // 查询可用的 V（cn + both，active + verified）
        List<Vtuber> candidates = vtuberMapper.selectList(
                new QueryWrapper<Vtuber>()
                        .in("data_status", "active", "verified")
                        .in("market", "cn", "both"));

        if (candidates.isEmpty()) {
            log.warn("无可用 VTuber，跳过题库初始化");
            return;
        }

        // 1. 创建每日题库
        Pool dailyPool = new Pool();
        dailyPool.setName("中文每日精选");
        dailyPool.setDescription("每日挑战题库，精选中文市场 VTuber");
        dailyPool.setMarket("cn");
        dailyPool.setMode("daily");
        dailyPool.setIsActive(true);
        dailyPool.setSortOrder(1);
        poolMapper.insert(dailyPool);

        // 2. 创建单人大题库
        Pool singlePool = new Pool();
        singlePool.setName("中文全量");
        singlePool.setDescription("单人模式全量题库");
        singlePool.setMarket("cn");
        singlePool.setMode("single");
        singlePool.setIsActive(true);
        singlePool.setSortOrder(1);
        poolMapper.insert(singlePool);

        // 3. 把 V 加入两个题库
        for (Vtuber v : candidates) {
            PoolItem dailyItem = new PoolItem();
            dailyItem.setPoolId(dailyPool.getId());
            dailyItem.setVtuberId(v.getId());
            poolItemMapper.insert(dailyItem);

            PoolItem singleItem = new PoolItem();
            singleItem.setPoolId(singlePool.getId());
            singleItem.setVtuberId(v.getId());
            poolItemMapper.insert(singleItem);
        }

        log.info("题库初始化完成：每日 {} 条，单人 {} 条", candidates.size(), candidates.size());
    }
}
