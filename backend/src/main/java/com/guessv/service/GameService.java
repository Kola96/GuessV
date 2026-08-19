package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guessv.common.BizException;
import com.guessv.dto.*;
import com.guessv.entity.DailyTarget;
import com.guessv.entity.GameRecord;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.GameRecordMapper;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final DailyTargetService dailyTargetService;
    private final ComparisonService comparisonService;
    private final GameRecordMapper gameRecordMapper;
    private final VtuberMapper vtuberMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.game.max-attempts:8}")
    private int maxAttempts;

    // ===== 每日模式 =====

    public DailyGameInfoVO getDailyInfo(Long userId) {
        dailyTargetService.getOrCreateToday();
        GameRecord record = findDailyRecord(userId, LocalDate.now());
        long total = vtuberMapper.selectCount(
                new QueryWrapper<Vtuber>().in("data_status", "active", "verified"));
        List<GuessEntry> guesses = parseGuesses(record);
        boolean hasPlayed = record != null;
        boolean hasWon = record != null && Boolean.TRUE.equals(record.getIsWin());
        int used = record != null ? record.getAttempts() : 0;

        // 仅在游戏已结束时返回目标信息（防作弊）
        String targetName = null;
        String targetAvatarUrl = null;
        if (record != null && record.getFinishedAt() != null) {
            Vtuber target = vtuberMapper.selectById(record.getTargetId());
            if (target != null) {
                targetName = displayName(target);
                targetAvatarUrl = target.getAvatarUrl();
            }
        }

        return new DailyGameInfoVO(
                LocalDate.now().toString(), maxAttempts, (int) total,
                hasPlayed, hasWon, used, guesses, targetName, targetAvatarUrl);
    }

    @Transactional
    public GuessResponse dailyGuess(Long userId, Long vtuberId) {
        DailyTarget target = dailyTargetService.getOrCreateToday();
        GameRecord record = findDailyRecord(userId, LocalDate.now());
        if (record == null) record = createDailyRecord(userId, target.getVtuberId());

        if (record.getFinishedAt() != null) throw new BizException(409, "今日游戏已结束");
        if (record.getAttempts() >= maxAttempts) throw new BizException(409, "尝试次数已用完");

        List<GuessEntry> entries = parseGuesses(record);
        if (entries.stream().anyMatch(e -> e.vtuberId().equals(vtuberId)))
            throw new BizException(409, "你已经猜过这个 VTuber 了");

        Vtuber guess = vtuberMapper.selectById(vtuberId);
        if (guess == null) throw new BizException(404, "VTuber 不存在");
        Vtuber targetV = vtuberMapper.selectById(target.getVtuberId());
        ComparisonResult comparison = comparisonService.compare(guess, targetV);

        int attemptNo = entries.size() + 1;
        boolean correct = guess.getId().equals(targetV.getId());
        entries.add(new GuessEntry(vtuberId, displayName(guess), attemptNo, correct,
                comparison, LocalDateTime.now().toString()));

        record.setAttempts(attemptNo);
        record.setGuesses(serializeGuesses(entries));

        boolean gameOver = false, win = false;
        if (correct) {
            record.setIsWin(true);
            record.setFinishedAt(LocalDateTime.now());
            gameOver = true; win = true;
        } else if (attemptNo >= maxAttempts) {
            record.setIsWin(false);
            record.setFinishedAt(LocalDateTime.now());
            gameOver = true;
        }
        gameRecordMapper.updateById(record);

        GuessResponse.VtuberReveal reveal = gameOver
                ? new GuessResponse.VtuberReveal(targetV.getId(), displayName(targetV), targetV.getAvatarUrl())
                : null;
        return new GuessResponse(correct, gameOver, win,
                maxAttempts - attemptNo, attemptNo, comparison, reveal);
    }

    private GameRecord findDailyRecord(Long userId, LocalDate date) {
        return gameRecordMapper.selectOne(new QueryWrapper<GameRecord>()
                .eq("mode", "daily")
                .eq("user_id", userId)
                .eq("pool_tag", date.toString()));
    }

    private GameRecord createDailyRecord(Long userId, Long targetId) {
        GameRecord r = new GameRecord();
        r.setUserId(userId);
        r.setMode("daily");
        r.setTargetId(targetId);
        r.setPoolTag(LocalDate.now().toString());
        r.setAttempts(0);
        r.setMaxAttempts(maxAttempts);
        r.setIsWin(false);
        r.setGuesses("[]");
        r.setStartedAt(LocalDateTime.now());
        gameRecordMapper.insert(r);
        return r;
    }

    // ===== 单人模式 =====

    public List<PoolVO> listPools() {
        List<PoolVO> pools = new ArrayList<>();
        for (String tag : List.of("全量", "日V", "国V", "英语圈", "Hololive", "Nijisanji")) {
            long c = countByPool(tag);
            pools.add(new PoolVO(tag, describePool(tag), (int) c));
        }
        return pools;
    }

    private String describePool(String tag) {
        return switch (tag) {
            case "全量" -> "所有可用 VTuber";
            case "日V" -> "日本地区 VTuber";
            case "国V" -> "中国地区 VTuber";
            case "英语圈" -> "英语地区 VTuber";
            case "Hololive" -> "Hololive 所属";
            case "Nijisanji" -> "Nijisanji 所属";
            default -> tag;
        };
    }

    private long countByPool(String tag) {
        return findByPool(tag).size();
    }

    private List<Vtuber> findByPool(String tag) {
        QueryWrapper<Vtuber> qw = new QueryWrapper<Vtuber>()
                .in("data_status", "active", "verified");
        switch (tag) {
            case "日V" -> qw.eq("region", "日本");
            case "国V" -> qw.eq("region", "中国");
            case "英语圈" -> qw.eq("region", "英语圈");
            case "Hololive" -> qw.likeRight("group_name", "Hololive");
            case "Nijisanji" -> qw.likeRight("group_name", "Nijisanji");
            default -> {}
        }
        return vtuberMapper.selectList(qw);
    }

    @Transactional
    public SingleStartResponse startSingle(Long userId, String poolTag) {
        if (poolTag == null || poolTag.isBlank()) poolTag = "全量";
        List<Vtuber> candidates = findByPool(poolTag);
        if (candidates.isEmpty()) throw new BizException(400, "题库无可用 VTuber: " + poolTag);
        Vtuber picked = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        GameRecord r = new GameRecord();
        r.setUserId(userId);
        r.setMode("single");
        r.setTargetId(picked.getId());
        r.setPoolTag(poolTag);
        r.setAttempts(0);
        r.setMaxAttempts(maxAttempts);
        r.setIsWin(false);
        r.setGuesses("[]");
        r.setStartedAt(LocalDateTime.now());
        gameRecordMapper.insert(r);
        return new SingleStartResponse(r.getId(), maxAttempts, poolTag, candidates.size());
    }

    @Transactional
    public GuessResponse singleGuess(Long userId, Long sessionId, Long vtuberId) {
        GameRecord record = gameRecordMapper.selectById(sessionId);
        if (record == null || !"single".equals(record.getMode()))
            throw new BizException(404, "会话不存在");
        if (!record.getUserId().equals(userId))
            throw new BizException(403, "无权访问该会话");
        if (record.getFinishedAt() != null) throw new BizException(409, "本局已结束");
        if (record.getAttempts() >= maxAttempts) throw new BizException(409, "尝试次数已用完");

        List<GuessEntry> entries = parseGuesses(record);
        if (entries.stream().anyMatch(e -> e.vtuberId().equals(vtuberId)))
            throw new BizException(409, "你已经猜过这个 VTuber 了");

        Vtuber guess = vtuberMapper.selectById(vtuberId);
        if (guess == null) throw new BizException(404, "VTuber 不存在");
        Vtuber targetV = vtuberMapper.selectById(record.getTargetId());
        ComparisonResult comparison = comparisonService.compare(guess, targetV);

        int attemptNo = entries.size() + 1;
        boolean correct = guess.getId().equals(targetV.getId());
        entries.add(new GuessEntry(vtuberId, displayName(guess), attemptNo, correct,
                comparison, LocalDateTime.now().toString()));

        record.setAttempts(attemptNo);
        record.setGuesses(serializeGuesses(entries));

        boolean gameOver = false, win = false;
        if (correct) {
            record.setIsWin(true);
            record.setFinishedAt(LocalDateTime.now());
            gameOver = true; win = true;
        } else if (attemptNo >= maxAttempts) {
            record.setIsWin(false);
            record.setFinishedAt(LocalDateTime.now());
            gameOver = true;
        }
        gameRecordMapper.updateById(record);

        GuessResponse.VtuberReveal reveal = gameOver
                ? new GuessResponse.VtuberReveal(targetV.getId(), displayName(targetV), targetV.getAvatarUrl())
                : null;
        return new GuessResponse(correct, gameOver, win,
                maxAttempts - attemptNo, attemptNo, comparison, reveal);
    }

    public DailyGameInfoVO getSingleState(Long userId, Long sessionId) {
        GameRecord record = gameRecordMapper.selectById(sessionId);
        if (record == null || !"single".equals(record.getMode()))
            throw new BizException(404, "会话不存在");
        if (!record.getUserId().equals(userId))
            throw new BizException(403, "无权访问该会话");
        List<GuessEntry> guesses = parseGuesses(record);
        boolean hasWon = Boolean.TRUE.equals(record.getIsWin());

        String targetName = null;
        String targetAvatarUrl = null;
        if (record.getFinishedAt() != null) {
            Vtuber target = vtuberMapper.selectById(record.getTargetId());
            if (target != null) {
                targetName = displayName(target);
                targetAvatarUrl = target.getAvatarUrl();
            }
        }

        return new DailyGameInfoVO(
                record.getPoolTag(), maxAttempts, 0,
                true, hasWon, record.getAttempts(), guesses, targetName, targetAvatarUrl);
    }

    // ===== 工具 =====

    private List<GuessEntry> parseGuesses(GameRecord r) {
        if (r == null || r.getGuesses() == null) return new ArrayList<>();
        try {
            Object raw = r.getGuesses();
            if (raw instanceof String s) {
                return objectMapper.readValue(s, new TypeReference<>() {});
            }
            return objectMapper.convertValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析 guesses 失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String serializeGuesses(List<GuessEntry> entries) {
        try {
            return objectMapper.writeValueAsString(entries);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String displayName(Vtuber v) {
        if ("cn".equals(v.getNameDefault()) && v.getNameCn() != null) return v.getNameCn();
        if (v.getNameEn() != null) return v.getNameEn();
        return v.getNameCn();
    }
}
