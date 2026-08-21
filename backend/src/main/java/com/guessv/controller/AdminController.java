package com.guessv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.common.Result;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.VtuberMapper;
import com.guessv.service.AdminService;
import com.guessv.service.FollowerSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final VtuberMapper vtuberMapper;
    private final FollowerSyncService followerSyncService;

    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody LoginRequest req) {
        String token = adminService.login(req.username(), req.password());
        return Result.ok(Map.of("token", token));
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        long raw = vtuberMapper.selectCount(new QueryWrapper<Vtuber>().eq("data_status", "raw"));
        long candidate = vtuberMapper.selectCount(new QueryWrapper<Vtuber>().eq("data_status", "candidate"));
        long active = vtuberMapper.selectCount(new QueryWrapper<Vtuber>().eq("data_status", "active"));
        long verified = vtuberMapper.selectCount(new QueryWrapper<Vtuber>().eq("data_status", "verified"));
        long withFollowers = vtuberMapper.selectCount(new QueryWrapper<Vtuber>().isNotNull("follower_count"));
        return Result.ok(Map.of(
                "raw", raw, "candidate", candidate,
                "active", active, "verified", verified,
                "total", raw + candidate + active + verified,
                "withFollowers", withFollowers
        ));
    }

    @PostMapping("/sync/followers")
    public Result<Map<String, Integer>> syncFollowers() {
        return Result.ok(followerSyncService.syncFollowers());
    }

    public record LoginRequest(String username, String password) {}
}
