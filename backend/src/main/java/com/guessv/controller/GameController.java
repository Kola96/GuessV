package com.guessv.controller;

import com.guessv.common.Result;
import com.guessv.dto.DailyGameInfoVO;
import com.guessv.dto.GuessResponse;
import com.guessv.dto.PoolVO;
import com.guessv.dto.SingleStartResponse;
import com.guessv.entity.User;
import com.guessv.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    // ===== 每日模式 =====

    @GetMapping("/daily")
    public Result<DailyGameInfoVO> dailyInfo(@RequestAttribute("currentUser") User user) {
        return Result.ok(gameService.getDailyInfo(user.getId()));
    }

    @PostMapping("/daily/guess")
    public Result<GuessResponse> dailyGuess(
            @RequestAttribute("currentUser") User user,
            @RequestBody DailyGuessRequest req) {
        return Result.ok(gameService.dailyGuess(user.getId(), req.vtuberId()));
    }

    public record DailyGuessRequest(Long vtuberId) {}

    // ===== 单人模式 =====

    @GetMapping("/single/pools")
    public Result<List<PoolVO>> pools() {
        return Result.ok(gameService.listPools());
    }

    @PostMapping("/single/start")
    public Result<SingleStartResponse> startSingle(
            @RequestAttribute("currentUser") User user,
            @RequestBody StartSingleRequest req) {
        return Result.ok(gameService.startSingle(user.getId(), req.poolTag()));
    }

    @PostMapping("/single/guess")
    public Result<GuessResponse> singleGuess(
            @RequestAttribute("currentUser") User user,
            @RequestBody SingleGuessRequest req) {
        return Result.ok(gameService.singleGuess(user.getId(), req.sessionId(), req.vtuberId()));
    }

    @GetMapping("/single/{sessionId}")
    public Result<DailyGameInfoVO> singleState(
            @RequestAttribute("currentUser") User user,
            @PathVariable Long sessionId) {
        return Result.ok(gameService.getSingleState(user.getId(), sessionId));
    }

    public record StartSingleRequest(String poolTag) {}
    public record SingleGuessRequest(Long sessionId, Long vtuberId) {}
}
