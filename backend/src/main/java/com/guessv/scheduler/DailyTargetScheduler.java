package com.guessv.scheduler;

import com.guessv.service.DailyTargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyTargetScheduler {

    private final DailyTargetService dailyTargetService;

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Shanghai")
    public void refreshDailyTarget() {
        log.info("定时任务：刷新每日目标");
        dailyTargetService.getOrCreateForDate(LocalDate.now());
    }
}
