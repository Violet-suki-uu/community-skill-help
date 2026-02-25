package com.rita.community.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rita.community.common.Result;
import com.rita.community.dto.SkillCreateReq;
import com.rita.community.dto.SkillDetailResp;
import com.rita.community.dto.SkillListItemResp;
import com.rita.community.dto.SkillUpdateReq;
import com.rita.community.entity.Skill;
import com.rita.community.entity.UserEvent;
import com.rita.community.entity.User;
import com.rita.community.mapper.UserEventMapper;
import com.rita.community.mapper.UserMapper;
import com.rita.community.service.SkillService;
import com.rita.community.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.time.LocalDateTime;

/**
 * SkillController
 * 作用：技能控制器，处理技能发布、列表、详情、编辑、上下架和删除。
 */
@RestController
@RequestMapping("/api/skills")
public class SkillController {
    private static final Logger log = LoggerFactory.getLogger(SkillController.class);

    private final SkillService skillService;
    private final UserMapper userMapper;
    private final UserEventMapper userEventMapper;

    public SkillController(SkillService skillService, UserMapper userMapper, UserEventMapper userEventMapper) {
        this.skillService = skillService;
        this.userMapper = userMapper;
        this.userEventMapper = userEventMapper;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Login required");
        }
        String token = auth.substring(7).trim();
        try {
            return JwtUtil.getUserId(token);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody SkillCreateReq req, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            Skill s = new Skill();
            s.setUserId(userId);
            s.setTitle(req.getTitle());
            s.setDescription(req.getDescription());
            s.setImageUrl(req.getImageUrl());
            s.setCategory(req.getCategory());
            s.setPrice(req.getPrice());
            s.setStatus(1);
            s.setLng(req.getLng());
            s.setLat(req.getLat());
            s.setAddress(req.getAddress());
            s.setAdcode(req.getAdcode());
            s.setCityName(req.getCityName());

            Long id = skillService.create(s);
            return Result.ok(id);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping
    public Result<IPage<SkillListItemResp>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category
    ) {
        IPage<Skill> data = skillService.page(new Page<>(page, size), keyword, category);
        Page<SkillListItemResp> resultPage = new Page<>(data.getCurrent(), data.getSize(), data.getTotal());
        resultPage.setRecords(toListResp(data.getRecords()));
        return Result.ok(resultPage);
    }

    @GetMapping("/{id}")
    public Result<SkillDetailResp> detail(@PathVariable Long id, HttpServletRequest request) {
        Skill skill = skillService.getById(id);
        if (skill == null) {
            return Result.fail("Skill not found");
        }

        skillService.increaseViewCount(id);
        int newViewCount = (skill.getViewCount() == null ? 0 : skill.getViewCount()) + 1;
        skill.setViewCount(newViewCount);

        Long userId = getOptionalUserId(request);
        if (userId != null) {
            recordUserEvent(userId, "view", id, null);
        }

        User seller = userMapper.selectById(skill.getUserId());

        SkillDetailResp resp = new SkillDetailResp();
        resp.setId(skill.getId());
        resp.setUserId(skill.getUserId());
        resp.setTitle(skill.getTitle());
        resp.setDescription(skill.getDescription());
        resp.setCategory(skill.getCategory());
        resp.setPrice(skill.getPrice());
        resp.setStatus(skill.getStatus());
        resp.setCreatedAt(skill.getCreatedAt());
        resp.setUpdatedAt(skill.getUpdatedAt());
        resp.setImageUrl(skill.getImageUrl());
        resp.setLng(skill.getLng());
        resp.setLat(skill.getLat());
        resp.setAddress(skill.getAddress());
        resp.setAdcode(skill.getAdcode());
        resp.setCityName(skill.getCityName());
        resp.setViewCount(skill.getViewCount());
        resp.setSellerNickname(seller == null ? "User" : seller.getNickname());
        resp.setSellerCreditScore(seller == null ? 0 : seller.getCreditScore());
        return Result.ok(resp);
    }

    @GetMapping("/mine")
    public Result<List<Skill>> mine(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.ok(skillService.mine(userId));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body,
                                        HttpServletRequest request) {
        return doUpdateStatus(id, body, request);
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateSkill(@PathVariable Long id, @Valid @RequestBody SkillUpdateReq req,
                                       HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            Skill update = new Skill();
            update.setTitle(req.getTitle());
            update.setDescription(req.getDescription());
            update.setCategory(req.getCategory());
            update.setPrice(req.getPrice());
            update.setImageUrl(req.getImageUrl());
            update.setLng(req.getLng());
            update.setLat(req.getLat());
            update.setAddress(req.getAddress());
            update.setAdcode(req.getAdcode());
            update.setCityName(req.getCityName());

            boolean ok = skillService.updateByOwner(id, userId, update);
            return ok ? Result.ok(true) : ownerCheckFail(id, userId);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean ok = skillService.removeByOwner(id, userId);
            return ok ? Result.ok(true) : ownerCheckFail(id, userId);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    private Result<Boolean> doUpdateStatus(Long id, Map<String, Integer> body, HttpServletRequest request) {
        try {
            Integer status = body == null ? null : body.get("status");
            if (status == null || (status != 0 && status != 1)) {
                return Result.fail("status must be 0 or 1");
            }
            Long userId = getCurrentUserId(request);
            boolean ok = skillService.updateStatus(id, userId, status);
            return ok ? Result.ok(true) : ownerCheckFail(id, userId);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    private List<SkillListItemResp> toListResp(List<Skill> skills) {
        List<SkillListItemResp> records = new ArrayList<>();
        if (skills == null || skills.isEmpty()) {
            return records;
        }

        Set<Long> userIds = new HashSet<>();
        for (Skill skill : skills) {
            if (skill.getUserId() != null) {
                userIds.add(skill.getUserId());
            }
        }

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) {
                userMap.put(user.getId(), user);
            }
        }

        for (Skill skill : skills) {
            User seller = userMap.get(skill.getUserId());
            SkillListItemResp item = new SkillListItemResp();
            item.setId(skill.getId());
            item.setUserId(skill.getUserId());
            item.setTitle(skill.getTitle());
            item.setDescription(skill.getDescription());
            item.setCategory(skill.getCategory());
            item.setPrice(skill.getPrice());
            item.setStatus(skill.getStatus());
            item.setCreatedAt(skill.getCreatedAt());
            item.setUpdatedAt(skill.getUpdatedAt());
            item.setImageUrl(skill.getImageUrl());
            item.setLng(skill.getLng());
            item.setLat(skill.getLat());
            item.setAddress(skill.getAddress());
            item.setAdcode(skill.getAdcode());
            item.setCityName(skill.getCityName());
            item.setViewCount(skill.getViewCount());
            item.setSellerNickname(seller == null ? "User" : seller.getNickname());
            item.setSellerCreditScore(seller == null ? 0 : seller.getCreditScore());
            records.add(item);
        }

        return records;
    }

    private Result<Boolean> ownerCheckFail(Long skillId, Long currentUserId) {
        Skill skill = skillService.getById(skillId);
        if (skill == null) {
            return Result.fail("技能不存在");
        }
        if (!Objects.equals(skill.getUserId(), currentUserId)) {
            log.warn("Owner mismatch, skillId={}, skillUserId={}, currentUserId={}",
                    skillId, skill.getUserId(), currentUserId);
            return Result.fail("当前登录账号不是该技能发布者，请重新登录正确账号");
        }
        return Result.fail("操作失败，请稍后重试");
    }

    private Long getOptionalUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        String token = auth.substring(7).trim();
        try {
            return JwtUtil.getUserId(token);
        } catch (Exception ignore) {
            return null;
        }
    }

    private void recordUserEvent(Long userId, String eventType, Long skillId, String keyword) {
        if (userId == null || eventType == null || eventType.isBlank()) {
            return;
        }
        UserEvent event = new UserEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setSkillId(skillId);
        event.setKeyword(keyword);
        event.setCreatedAt(LocalDateTime.now());
        userEventMapper.insert(event);
    }
}

