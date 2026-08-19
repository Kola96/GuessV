package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.entity.DailyTarget;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.DailyTargetMapper;
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

    @Transactional
    public DailyTarget getOrCreateToday() {
        return getOrCreateForDate(LocalDate.now());
    }

    @Transactional
    public DailyTarget getOrCreateForDate(LocalDate date) {
        DailyTarget existing = dailyTargetMapper.selectOne(
                new QueryWrapper<DailyTarget>().eq("target_date", date.toString()));
        if (existing != null) return existing;

        LocalDate since = date.minusDays(EXCLUDE_RECENT_DAYS);
        List<DailyTarget> recent = dailyTargetMapper.selectList(
                new QueryWrapper<DailyTarget>()
                        .gt("target_date", since.toString())
                        .le("target_date", date.toString()));
        List<Long> recentIds = recent.stream()
                .map(DailyTarget::getVtuberId)
                .collect(Collectors.toList());

        QueryWrapper<Vtuber> qw = new QueryWrapper<Vtuber>()
                .in("data_status", "active", "verified");
        if (!recentIds.isEmpty()) qw.notIn("id", recentIds);
        List<Vtuber> candidates = vtuberMapper.selectList(qw);

        if (candidates.isEmpty()) {
            candidates = vtuberMapper.selectList(
                    new QueryWrapper<Vtuber>().in("data_status", "active", "verified"));
        }
        if (candidates.isEmpty()) throw new IllegalStateException("无可用 VTuber 作为每日目标");

        Vtuber picked = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        DailyTarget t = new DailyTarget();
        t.setTargetDate(date);
        t.setVtuberId(picked.getId());
        dailyTargetMapper.insert(t);
        log.info("每日目标已生成：date={} vtuberId={}", date, picked.getId());
        return t;
    }

    public DailyTarget getByDate(LocalDate date) {
        return dailyTargetMapper.selectOne(
                new QueryWrapper<DailyTarget>().eq("target_date", date.toString()));
    }
}
