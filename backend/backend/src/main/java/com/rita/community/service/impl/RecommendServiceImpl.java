package com.rita.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rita.community.dto.RecommendPageResp;
import com.rita.community.dto.SkillListItemResp;
import com.rita.community.entity.Skill;
import com.rita.community.entity.User;
import com.rita.community.entity.UserEvent;
import com.rita.community.mapper.SkillMapper;
import com.rita.community.mapper.UserEventMapper;
import com.rita.community.mapper.UserMapper;
import com.rita.community.service.RecommendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RecommendServiceImpl
 * 作用：推荐业务实现，基于用户行为与技能特征计算推荐结果。
 */
@Service
public class RecommendServiceImpl implements RecommendService {
    private static final Logger log = LoggerFactory.getLogger(RecommendServiceImpl.class);
    private static final int PROFILE_EVENT_LIMIT = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MIN_RECALL_SIZE = 20;
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final SkillMapper skillMapper;
    private final UserMapper userMapper;
    private final UserEventMapper userEventMapper;

    public RecommendServiceImpl(SkillMapper skillMapper, UserMapper userMapper, UserEventMapper userEventMapper) {
        this.skillMapper = skillMapper;
        this.userMapper = userMapper;
        this.userEventMapper = userEventMapper;
    }

    @Override
    public RecommendPageResp recommend(Long userId, String cursor, Integer size) {
        int pageSize = normalizePageSize(size);

        List<Skill> activeSkills = skillMapper.selectList(new LambdaQueryWrapper<Skill>()
                .eq(Skill::getStatus, 1));
        if (activeSkills.isEmpty()) {
            return new RecommendPageResp();
        }

        InterestProfile profile = buildProfile(userId);
        if (userId != null) {
            log.info("User {} interest tags => categories: {}, keywords: {}", userId, profile.topCategories, profile.topKeywords);
        } else {
            log.info("Guest recommend fallback => hot + fresh");
        }

        List<ScoredSkill> scored = scoreSkills(activeSkills, profile);
        scored.sort((a, b) -> {
            int scoreCmp = Double.compare(b.score, a.score);
            if (scoreCmp != 0) return scoreCmp;
            int timeCmp = Long.compare(b.createdEpoch, a.createdEpoch);
            if (timeCmp != 0) return timeCmp;
            return Long.compare(b.skillId, a.skillId);
        });

        CursorKey cursorKey = CursorKey.parse(cursor);
        if (cursorKey != null) {
            scored = scored.stream().filter((item) -> isAfterCursor(item, cursorKey)).collect(Collectors.toList());
        }

        int end = Math.min(pageSize, scored.size());
        List<ScoredSkill> pageItems = end == 0 ? Collections.emptyList() : scored.subList(0, end);
        Map<Long, User> sellerMap = loadSellerMap(pageItems);

        RecommendPageResp resp = new RecommendPageResp();
        List<SkillListItemResp> items = new ArrayList<>();
        for (ScoredSkill item : pageItems) {
            items.add(toListItem(item.skill, sellerMap.get(item.skill.getUserId())));
        }
        resp.setItems(items);

        if (scored.size() > end && !pageItems.isEmpty()) {
            resp.setNextCursor(toCursor(pageItems.get(pageItems.size() - 1)));
        } else {
            resp.setNextCursor(null);
        }
        return resp;
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private InterestProfile buildProfile(Long userId) {
        if (userId == null) return InterestProfile.empty();

        List<UserEvent> events = userEventMapper.selectList(new LambdaQueryWrapper<UserEvent>()
                .eq(UserEvent::getUserId, userId)
                .orderByDesc(UserEvent::getCreatedAt)
                .last("LIMIT " + PROFILE_EVENT_LIMIT));
        if (events.isEmpty()) return InterestProfile.empty();

        Set<Long> skillIds = new HashSet<>();
        for (UserEvent event : events) {
            if (event.getSkillId() != null) {
                skillIds.add(event.getSkillId());
            }
        }

        Map<Long, Skill> skillMap = skillIds.isEmpty()
                ? Collections.emptyMap()
                : skillMapper.selectBatchIds(skillIds).stream().collect(Collectors.toMap(Skill::getId, x -> x));

        Map<String, Integer> categoryCounter = new HashMap<>();
        Map<String, Integer> keywordCounter = new HashMap<>();

        for (UserEvent event : events) {
            String eventType = normalizeText(event.getEventType()).toLowerCase(Locale.ROOT);
            if (("view".equals(eventType) || "favorite".equals(eventType)) && event.getSkillId() != null) {
                Skill skill = skillMap.get(event.getSkillId());
                String category = skill == null ? "" : normalizeText(skill.getCategory());
                if (!category.isEmpty()) {
                    categoryCounter.merge(category, 1, Integer::sum);
                }
            }
            if ("search".equals(eventType)) {
                String keyword = normalizeText(event.getKeyword());
                if (!keyword.isEmpty()) {
                    keywordCounter.merge(keyword, 1, Integer::sum);
                }
            }
        }

        List<String> topCategories = pickTopKeys(categoryCounter, 3);
        List<String> topKeywords = pickTopKeys(keywordCounter, 3);
        return new InterestProfile(topCategories, topKeywords);
    }

    private List<String> pickTopKeys(Map<String, Integer> counter, int limit) {
        return counter.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<ScoredSkill> scoreSkills(List<Skill> skills, InterestProfile profile) {
        boolean hasInterest = profile.hasInterest();

        long newest = Long.MIN_VALUE;
        long oldest = Long.MAX_VALUE;
        int maxViewCount = 0;
        for (Skill skill : skills) {
            long epoch = toEpoch(skill.getCreatedAt());
            newest = Math.max(newest, epoch);
            oldest = Math.min(oldest, epoch);
            maxViewCount = Math.max(maxViewCount, safeViewCount(skill));
        }
        if (newest == Long.MIN_VALUE) newest = 0;
        if (oldest == Long.MAX_VALUE) oldest = 0;

        Set<Long> includedIds = new HashSet<>();
        List<ScoredSkill> scored = new ArrayList<>();

        for (Skill skill : skills) {
            boolean categoryMatched = containsCategory(profile.topCategories, skill.getCategory());
            boolean keywordMatched = containsKeyword(profile.topKeywords, skill);
            if (hasInterest && !(categoryMatched || keywordMatched)) {
                continue;
            }
            ScoredSkill item = scoreOne(skill, hasInterest, categoryMatched, keywordMatched, maxViewCount, newest, oldest);
            scored.add(item);
            includedIds.add(item.skillId);
        }

        if (hasInterest && scored.size() < MIN_RECALL_SIZE) {
            for (Skill skill : skills) {
                Long id = skill.getId();
                if (id == null || includedIds.contains(id)) continue;
                boolean categoryMatched = containsCategory(profile.topCategories, skill.getCategory());
                boolean keywordMatched = containsKeyword(profile.topKeywords, skill);
                scored.add(scoreOne(skill, true, categoryMatched, keywordMatched, maxViewCount, newest, oldest));
            }
        }

        return scored;
    }

    private ScoredSkill scoreOne(
            Skill skill,
            boolean hasInterest,
            boolean categoryMatched,
            boolean keywordMatched,
            int maxViewCount,
            long newest,
            long oldest
    ) {
        int viewCount = safeViewCount(skill);
        double hotScore = maxViewCount > 0 ? (double) viewCount / (double) maxViewCount : 0.0;

        long createdEpoch = toEpoch(skill.getCreatedAt());
        double freshnessScore;
        if (newest <= oldest) {
            freshnessScore = 1.0;
        } else {
            freshnessScore = (double) (createdEpoch - oldest) / (double) (newest - oldest);
            freshnessScore = Math.max(0.0, Math.min(1.0, freshnessScore));
        }

        double score;
        if (!hasInterest) {
            score = 0.6 * hotScore + 0.4 * freshnessScore;
        } else {
            double categorySignal = categoryMatched ? 1.0 : (keywordMatched ? 0.8 : 0.0);
            score = 0.5 * categorySignal + 0.3 * hotScore + 0.2 * freshnessScore;
        }

        Long id = skill.getId();
        return new ScoredSkill(skill, id == null ? 0L : id, score, createdEpoch);
    }

    private Map<Long, User> loadSellerMap(List<ScoredSkill> scoredSkills) {
        Set<Long> userIds = new HashSet<>();
        for (ScoredSkill scored : scoredSkills) {
            if (scored.skill.getUserId() != null) {
                userIds.add(scored.skill.getUserId());
            }
        }
        if (userIds.isEmpty()) return Collections.emptyMap();
        return userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, x -> x));
    }

    private SkillListItemResp toListItem(Skill skill, User seller) {
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
        return item;
    }

    private boolean containsCategory(List<String> categories, String category) {
        String target = normalizeText(category);
        if (target.isEmpty()) return false;
        for (String c : categories) {
            if (Objects.equals(c, target)) return true;
        }
        return false;
    }

    private boolean containsKeyword(List<String> keywords, Skill skill) {
        if (keywords.isEmpty()) return false;
        String title = normalizeText(skill.getTitle()).toLowerCase(Locale.ROOT);
        String desc = normalizeText(skill.getDescription()).toLowerCase(Locale.ROOT);
        for (String raw : keywords) {
            String keyword = normalizeText(raw).toLowerCase(Locale.ROOT);
            if (keyword.isEmpty()) continue;
            if (title.contains(keyword) || desc.contains(keyword)) return true;
        }
        return false;
    }

    private int safeViewCount(Skill skill) {
        return skill.getViewCount() == null ? 0 : Math.max(0, skill.getViewCount());
    }

    private long toEpoch(LocalDateTime time) {
        if (time == null) return 0L;
        return time.atZone(ZONE).toInstant().toEpochMilli();
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.trim();
    }

    private String toCursor(ScoredSkill scored) {
        return String.format(Locale.ROOT, "%.6f|%d|%d", scored.score, scored.createdEpoch, scored.skillId);
    }

    private boolean isAfterCursor(ScoredSkill scored, CursorKey cursor) {
        if (scored.score < cursor.score - 1e-9) return true;
        if (Math.abs(scored.score - cursor.score) <= 1e-9) {
            if (scored.createdEpoch < cursor.createdEpoch) return true;
            if (scored.createdEpoch == cursor.createdEpoch) {
                return scored.skillId < cursor.skillId;
            }
        }
        return false;
    }

    private static final class InterestProfile {
        private final List<String> topCategories;
        private final List<String> topKeywords;

        private InterestProfile(List<String> topCategories, List<String> topKeywords) {
            this.topCategories = topCategories;
            this.topKeywords = topKeywords;
        }

        private static InterestProfile empty() {
            return new InterestProfile(Collections.emptyList(), Collections.emptyList());
        }

        private boolean hasInterest() {
            return !topCategories.isEmpty() || !topKeywords.isEmpty();
        }
    }

    private static final class ScoredSkill {
        private final Skill skill;
        private final long skillId;
        private final double score;
        private final long createdEpoch;

        private ScoredSkill(Skill skill, long skillId, double score, long createdEpoch) {
            this.skill = skill;
            this.skillId = skillId;
            this.score = score;
            this.createdEpoch = createdEpoch;
        }
    }

    private static final class CursorKey {
        private final double score;
        private final long createdEpoch;
        private final long skillId;

        private CursorKey(double score, long createdEpoch, long skillId) {
            this.score = score;
            this.createdEpoch = createdEpoch;
            this.skillId = skillId;
        }

        private static CursorKey parse(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] parts = raw.split("\\|");
            if (parts.length != 3) return null;
            try {
                double score = Double.parseDouble(parts[0]);
                long createdEpoch = Long.parseLong(parts[1]);
                long skillId = Long.parseLong(parts[2]);
                return new CursorKey(score, createdEpoch, skillId);
            } catch (Exception ignore) {
                return null;
            }
        }
    }
}


