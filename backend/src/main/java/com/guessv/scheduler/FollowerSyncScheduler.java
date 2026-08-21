package com.guessv.scheduler;

import com.guessv.service.FollowerSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 粉丝量定时同步任务。
 * 每日凌晨 3:00 执行（错开每日目标刷新的 0:00）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FollowerSyncScheduler {

    private final FollowerSyncService followerSyncService;

    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Shanghai")
    public void syncFollowers() {
        log.info("定时任务：同步粉丝量");
        try {
            followerSyncService.syncFollowers();
        } catch (Exception e) {
            log.error("粉丝量定时同步失败: {}", e.getMessage(), e);
        }
    }
}
