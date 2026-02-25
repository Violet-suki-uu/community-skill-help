package com.rita.community.controller;

import com.rita.community.common.Result;
import com.rita.community.entity.UserEvent;
import com.rita.community.mapper.UserEventMapper;
import com.rita.community.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * UserEventController
 * 作用：用户行为控制器，记录浏览、搜索、收藏等行为数据。
 */
@RestController
@RequestMapping("/api/user-events")
public class UserEventController {
    private static final Set<String> ALLOWED_TYPES = Set.of("view", "search", "favorite");

    private final UserEventMapper userEventMapper;

    public UserEventController(UserEventMapper userEventMapper) {
        this.userEventMapper = userEventMapper;
    }

    @PostMapping
    public Result<Boolean> logEvent(@RequestBody EventReq req, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.fail("Unauthorized");
        }
        String eventType = req == null || req.getEventType() == null ? "" : req.getEventType().trim().toLowerCase();
        if (!ALLOWED_TYPES.contains(eventType)) {
            return Result.fail("eventType must be view/search/favorite");
        }
        String keyword = req == null || req.getKeyword() == null ? null : req.getKeyword().trim();
        if (keyword != null && keyword.isEmpty()) keyword = null;

        UserEvent event = new UserEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setSkillId(req == null ? null : req.getSkillId());
        event.setKeyword(keyword);
        event.setCreatedAt(LocalDateTime.now());
        userEventMapper.insert(event);
        return Result.ok(true);
    }

    @PostMapping("/search")
    public Result<Boolean> logSearch(@RequestBody SearchReq req, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.fail("Unauthorized");
        }
        String keyword = req == null || req.getKeyword() == null ? "" : req.getKeyword().trim();
        if (keyword.isEmpty()) {
            return Result.fail("keyword is required");
        }

        EventReq eventReq = new EventReq();
        eventReq.setEventType("search");
        eventReq.setKeyword(keyword);
        return logEvent(eventReq, request);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
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

    public static class SearchReq {
        private String keyword;

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
    }

    public static class EventReq {
        private String eventType;
        private Long skillId;
        private String keyword;

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public Long getSkillId() {
            return skillId;
        }

        public void setSkillId(Long skillId) {
            this.skillId = skillId;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
    }
}

