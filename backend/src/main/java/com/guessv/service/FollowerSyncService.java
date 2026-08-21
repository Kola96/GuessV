package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guessv.entity.CrawlLog;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.CrawlLogMapper;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 从 vtbs.moe API (https://api.vtbs.moe/v1/info) 同步粉丝量到数据库。
 * 一次调用获取全部 VTuber 的 follower/face/roomid 等数据，按 uuid 匹配更新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowerSyncService {

    private static final String API_URL = "https://api.vtbs.moe/v1/info";
    private static final int MAX_API_RETRIES = 3;

    private final VtuberMapper vtuberMapper;
    private final CrawlLogMapper crawlLogMapper;
    private final ObjectMapper objectMapper;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    // 并发锁：防止手动触发 + 定时任务同时执行
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 执行同步：拉取 API → 按 uuid 匹配 → 更新 follower_count + avatar_url
     * @return 同步统计
     */
    public Map<String, Integer> syncFollowers() {
        // 并发锁检查
        if (!running.compareAndSet(false, true)) {
            log.warn("粉丝量同步任务已在执行中，跳过本次触发");
            Map<String, Integer> skipped = new HashMap<>();
            skipped.put("total", -1);
            skipped.put("matched", -1);
            skipped.put("updated", -1);
            skipped.put("skipped", -1);
            skipped.put("alreadyRunning", 1);
            return skipped;
        }

        try {
            return doSync();
        } finally {
            running.set(false);
        }
    }

    private Map<String, Integer> doSync() {
        log.info("开始同步粉丝量数据...");
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("matched", 0);
        stats.put("updated", 0);
        stats.put("skipped", 0);

        try {
            // 1. 拉 API（带重试）
            String jsonBody = fetchApiWithRetry();
            JsonNode apiData = objectMapper.readTree(jsonBody);
            stats.put("total", apiData.size());
            log.info("API 返回 {} 条记录", apiData.size());

            // 2. 构建 uuid → apiData 映射
            Map<String, JsonNode> apiMap = new HashMap<>();
            for (JsonNode item : apiData) {
                String uuid = item.path("uuid").asText(null);
                if (uuid != null && !uuid.isEmpty()) {
                    apiMap.put(uuid, item);
                }
            }

            // 3. 分批查询数据库的 VTuber 并更新
            List<Vtuber> allVtubers = vtuberMapper.selectList(
                    new QueryWrapper<Vtuber>().select("id", "uuid", "follower_count", "avatar_url"));
            log.info("数据库中有 {} 条 VTuber 记录", allVtubers.size());

            int matched = 0, updated = 0, skipped = 0;
            for (Vtuber vtb : allVtubers) {
                JsonNode apiItem = apiMap.get(vtb.getUuid());
                if (apiItem == null) {
                    skipped++;
                    continue;
                }
                matched++;

                int follower = apiItem.path("follower").asInt(0);
                String face = apiItem.path("face").asText(null);

                boolean changed = false;
                if (follower > 0 && (vtb.getFollowerCount() == null || vtb.getFollowerCount() != follower)) {
                    vtb.setFollowerCount(follower);
                    changed = true;
                }
                if (face != null && !face.isEmpty() && !face.equals(vtb.getAvatarUrl())) {
                    vtb.setAvatarUrl(face);
                    changed = true;
                }

                if (changed) {
                    vtb.setUpdatedAt(LocalDateTime.now());
                    vtuberMapper.updateById(vtb);
                    updated++;
                }
            }

            stats.put("matched", matched);
            stats.put("updated", updated);
            stats.put("skipped", skipped);

            // 4. 记录日志
            CrawlLog logEntry = new CrawlLog();
            logEntry.setSource("vtbs.moe/info");
            logEntry.setStatus("success");
            logEntry.setFieldsUpdated(List.of("follower_count", "avatar_url"));
            logEntry.setCreatedAt(LocalDateTime.now());
            crawlLogMapper.insert(logEntry);

            log.info("粉丝量同步完成：API {} 条，匹配 {}，更新 {}，跳过 {}", stats.get("total"), matched, updated, skipped);
            return stats;

        } catch (Exception e) {
            log.error("粉丝量同步失败: {}", e.getMessage(), e);

            CrawlLog logEntry = new CrawlLog();
            logEntry.setSource("vtbs.moe/info");
            logEntry.setStatus("failed");
            logEntry.setErrorMessage(e.getMessage());
            logEntry.setCreatedAt(LocalDateTime.now());
            crawlLogMapper.insert(logEntry);

            throw new RuntimeException("粉丝量同步失败: " + e.getMessage(), e);
        }
    }

    /**
     * 带 API 重试的拉取
     */
    private String fetchApiWithRetry() throws Exception {
        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_API_RETRIES; attempt++) {
            try {
                Request request = new Request.Builder()
                        .url(API_URL)
                        .header("User-Agent", "GuessV/1.0")
                        .get()
                        .build();

                try (Response resp = httpClient.newCall(request).execute()) {
                    if (!resp.isSuccessful()) {
                        throw new RuntimeException("API 返回 " + resp.code());
                    }
                    String body = resp.body() != null ? resp.body().string() : "[]";
                    log.info("API 拉取成功（第 {} 次尝试），{} 字节", attempt, body.length());
                    return body;
                }
            } catch (Exception e) {
                lastError = e;
                log.warn("API 拉取失败（第 {}/{} 次）: {}", attempt, MAX_API_RETRIES, e.getMessage());
                if (attempt < MAX_API_RETRIES) {
                    Thread.sleep(5000L * attempt);  // 递增等待
                }
            }
        }
        throw new RuntimeException("API 拉取失败，重试 " + MAX_API_RETRIES + " 次后仍不成功", lastError);
    }
}
