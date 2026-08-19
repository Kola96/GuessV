package com.guessv.controller;

import com.guessv.common.Result;
import com.guessv.dto.*;
import com.guessv.entity.User;
import com.guessv.service.NicknameService;
import com.guessv.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final NicknameService nicknameService;
    private final UserService userService;

    @GetMapping("/nickname/random")
    public Result<List<String>> randomNicknames(@RequestParam(defaultValue = "5") int count) {
        if (count <= 0 || count > 10) count = 5;
        List<String> names = IntStream.range(0, count)
                .mapToObj(i -> nicknameService.generateRandom())
                .toList();
        return Result.ok(names);
    }

    @PostMapping("/init")
    public Result<UserInitResponse> init(@Valid @RequestBody UserInitRequest req) {
        String nickname = Boolean.TRUE.equals(req.useRandomNickname()) ? null : req.nickname();
        var resp = userService.createAnonymousUser(nickname, req.deviceFingerprint());
        return Result.ok(resp);
    }

    @GetMapping("/profile")
    public Result<UserProfileVO> profile(
            @RequestAttribute("currentUser") User user) {
        return Result.ok(userService.getProfile(user));
    }

    @GetMapping("/nickname/check")
    public Result<NicknameCheckResponse> checkNickname(@RequestParam String nickname) {
        return Result.ok(userService.checkNickname(nickname));
    }

    @PutMapping("/nickname")
    public Result<UserProfileVO> updateNickname(
            @RequestAttribute("currentUser") User user,
            @Valid @RequestBody UpdateNicknameRequest req) {
        return Result.ok(userService.changeNickname(user, req.nickname()));
    }
}
