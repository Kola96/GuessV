package com.guessv.controller;

import com.guessv.common.Result;
import com.guessv.dto.VtuberSearchVO;
import com.guessv.service.VtuberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vtuber")
@RequiredArgsConstructor
public class VtuberController {

    private final VtuberService vtuberService;

    @GetMapping("/search")
    public Result<List<VtuberSearchVO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(vtuberService.search(keyword, limit));
    }
}
