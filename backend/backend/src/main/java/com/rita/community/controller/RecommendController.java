package com.rita.community.controller;

import com.rita.community.common.Result;
import com.rita.community.dto.RecommendPageResp;
import com.rita.community.service.RecommendService;
import com.rita.community.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RecommendController
 * 作用：推荐控制器，返回个性化技能推荐列表。
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {
    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping
    public Result<RecommendPageResp> recommend(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest request
    ) {
        Long userId = getOptionalUserId(request);
        return Result.ok(recommendService.recommend(userId, cursor, size));
    }

    private Long getOptionalUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String token = auth.substring(7).trim();
        try {
            return JwtUtil.getUserId(token);
        } catch (Exception ignore) {
            return null;
        }
    }
}


