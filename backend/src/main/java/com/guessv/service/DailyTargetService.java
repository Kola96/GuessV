package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.entity.DailyTarget;
import com.guessv.entity.Pool;
import com.guessv.entity.PoolItem;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.DailyTargetMapper;
import com.guessv.mapper.PoolItemMapper;
import com.guessv.mapper.PoolMapper;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyTargetService {

    private static final int EXCLUDE_RECENT_DAYS = 30;

    private final DailyTargetMapper dailyTargetMapper;
    private final VtuberMapper vtuberMapper;
    private final PoolMapper poolMapper;
    private final PoolItemMapper poolItemMapper;

    @Transactional
    public DailyTarget getOrCreateToday() {
        return getOrCreateForDate(LocalDate.now());
    }

    @Transactional
    public DailyTarget getOrCreateForDate(LocalDate date) {
        DailyTarget existing = dailyTargetMapper.selectOne(
                new QueryWrapper<DailyTarget>().eq("target_date", date.toString()));
        if (existing != null) return existing;

        // 从绑定的每日题库中选目标
        List<Long> candidateIds = getDailyPoolVtuberIds();

        // 排除近 N 天已选过的
        LocalDate since = date.minusDays(EXCLUDE_RECENT_DAYS);
        List<DailyTarget> recent = dailyTargetMapper.selectList(
                new QueryWrapper<DailyTarget>()
                        .gt("target_date", since.toString())
                        .le("target_date", date.toString()));
        List<Long> recentIds = recent.stream()
                .map(DailyTarget::getVtuberId)
                .collect(Collectors.toList());

        if (!recentIds.isEmpty()) {
            candidateIds = candidateIds.stream()
                    .filter(id -> !recentIds.contains(id))
                    .collect(Collectors.toList());
        }

        if (candidateIds.isEmpty()) {
            // 兜底：不排除近期
            candidateIds = getDailyPoolVtuberIds();
        }
        if (candidateIds.isEmpty()) {
            throw new IllegalStateException("每日题库为空，请先在后台添加题库成员");
        }

        Long pickedId = candidateIds.get(ThreadLocalRandom.current().nextInt(candidateIds.size()));
        DailyTarget t = new DailyTarget();
        t.setTargetDate(date);
        t.setVtuberId(pickedId);
        dailyTargetMapper.insert(t);
        log.info("每日目标已生成：date={} vtuberId={}", date, pickedId);
        return t;
    }

    public DailyTarget getByDate(LocalDate date) {
        return dailyTargetMapper.selectOne(
                new QueryWrapper<DailyTarget>().eq("target_date", date.toString()));
    }

    /**
     * 获取绑定的每日题库中的 VTuber ID 列表。
     * 查找 mode=daily 且 market 包含当前市场的活跃题库。
     */
    private List<Long> getDailyPoolVtuberIds() {
        List<Pool> dailyPools = poolMapper.selectList(
                new QueryWrapper<Pool>()
                        .eq("mode", "daily")
                        .eq("is_active", true)
                        .in("market", "cn", "all"));
        if (dailyPools.isEmpty()) {
            log.warn("未找到每日题库，请先在后台创建");
            return List.of();
        }
        List<Long> poolIds = dailyPools.stream().map(Pool::getId).toList();
        List<PoolItem> items = poolItemMapper.selectList(
                new QueryWrapper<PoolItem>().in("pool_id", poolIds));
        return items.stream().map(PoolItem::getVtuberId).distinct().toList();
    }
}
