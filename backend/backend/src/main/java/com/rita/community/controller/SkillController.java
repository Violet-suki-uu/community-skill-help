package com.rita.community.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rita.community.common.Result;
import com.rita.community.dto.SkillCreateReq;
import com.rita.community.entity.Skill;
import com.rita.community.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.rita.community.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    // 发布技能
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SkillCreateReq req, HttpServletRequest request) {

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Result.fail("未登录");
        }

        String token = auth.substring(7);
        Long userId;
        try {
            userId = JwtUtil.getUserId(token);
        } catch (Exception e) {
            return Result.fail("登录已失效，请重新登录");
        }

        Skill s = new Skill();
        s.setUserId(userId); // 不再写死
        s.setTitle(req.getTitle());
        s.setDescription(req.getDescription());
        s.setImageUrl(req.getImageUrl());
        s.setCategory(req.getCategory());
        s.setPrice(req.getPrice());
        s.setStatus(1);

        Long id = skillService.create(s);
        return Result.ok(id);
    }

    // 技能列表（分页 + 搜索）
    @GetMapping
    public Result<IPage<Skill>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category
    ) {
        IPage<Skill> data = skillService.page(new Page<>(page, size), keyword, category);
        return Result.ok(data);
    }
}
