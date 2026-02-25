package com.rita.community.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rita.community.common.Result;
import com.rita.community.dto.SkillCreateReq;
import com.rita.community.dto.SkillWithSellerResp;
import com.rita.community.entity.Skill;
import com.rita.community.entity.User;
import com.rita.community.mapper.UserMapper;
import com.rita.community.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.rita.community.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;
    private final UserMapper userMapper;

    public SkillController(SkillService skillService, UserMapper userMapper) {
        this.skillService = skillService;
        this.userMapper = userMapper;
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
    public Result<IPage<SkillWithSellerResp>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category
    ) {
        IPage<Skill> data = skillService.page(new Page<>(page, size), keyword, category);
        Page<SkillWithSellerResp> resultPage = new Page<>(data.getCurrent(), data.getSize(), data.getTotal());
        resultPage.setRecords(toSkillRespList(data.getRecords()));
        return Result.ok(resultPage);
    }

    @GetMapping("/{id}")
    public Result<SkillWithSellerResp> detail(@PathVariable("id") Long id) {
        Skill skill = skillService.getById(id);
        if (skill == null) {
            return Result.fail("Skill not found");
        }
        User seller = skill.getUserId() == null ? null : userMapper.selectById(skill.getUserId());
        return Result.ok(toSkillResp(skill, seller));
    }

    @GetMapping("/mine")
    public Result<List<Skill>> mine(HttpServletRequest request) {
        Long userId = extractUserId(request);
        if (userId == null) {
            return Result.fail("Unauthorized");
        }
        return Result.ok(skillService.listMine(userId));
    }

    @PatchMapping("/{id}/status")
    public Result<Boolean> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody StatusReq req,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        if (userId == null) {
            return Result.fail("Unauthorized");
        }
        if (req == null || req.getStatus() == null) {
            return Result.fail("Missing status");
        }
        boolean ok = skillService.updateStatus(userId, id, req.getStatus());
        return ok ? Result.ok(true) : Result.fail("Not found or not owner");
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateStatusFallback(
            @PathVariable("id") Long id,
            @RequestBody StatusReq req,
            HttpServletRequest request
    ) {
        return updateStatus(id, req, request);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @PathVariable("id") Long id,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        if (userId == null) {
            return Result.fail("Unauthorized");
        }
        boolean ok = skillService.removeByOwner(userId, id);
        return ok ? Result.ok(true) : Result.fail("Not found or not owner");
    }

    private Long extractUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        String token = auth.substring(7);
        try {
            return JwtUtil.getUserId(token);
        } catch (Exception e) {
            return null;
        }
    }

    private List<SkillWithSellerResp> toSkillRespList(List<Skill> skills) {
        List<SkillWithSellerResp> result = new ArrayList<>();
        if (skills == null || skills.isEmpty()) {
            return result;
        }

        Set<Long> userIds = new HashSet<>();
        for (Skill skill : skills) {
            if (skill.getUserId() != null) {
                userIds.add(skill.getUserId());
            }
        }

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }

        for (Skill skill : skills) {
            result.add(toSkillResp(skill, userMap.get(skill.getUserId())));
        }
        return result;
    }

    private SkillWithSellerResp toSkillResp(Skill skill, User seller) {
        SkillWithSellerResp resp = new SkillWithSellerResp();
        resp.setId(skill.getId());
        resp.setUserId(skill.getUserId());
        resp.setTitle(skill.getTitle());
        resp.setDescription(skill.getDescription());
        resp.setCategory(skill.getCategory());
        resp.setPrice(skill.getPrice());
        resp.setStatus(skill.getStatus());
        resp.setImageUrl(skill.getImageUrl());
        resp.setSellerNickname(seller == null || seller.getNickname() == null ? "User" : seller.getNickname());
        resp.setSellerCreditScore(seller == null || seller.getCreditScore() == null ? 0 : seller.getCreditScore());
        return resp;
    }

    public static class StatusReq {
        private Integer status;

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }
}
