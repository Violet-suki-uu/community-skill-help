package com.rita.community.service.impl;

import com.rita.community.service.CacheService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * CacheServiceImpl
 * 作用：基于 Spring Data Redis 的 CacheService 实现，封装 Key 规范与 TTL 策略。
 */
@Service
public class CacheServiceImpl implements CacheService {

    private static final String KEY_TOKEN_BLACKLIST = "auth:token:bl:";
    private static final String KEY_LOGIN_FAIL      = "auth:login:fail:";
    private static final String KEY_LOGIN_LOCK      = "auth:login:lock:";
    private static final String KEY_SKILL_DETAIL    = "skill:detail:";
    private static final String KEY_SKILL_VIEW      = "skill:view:";

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /* ============ 通用 ============ */

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object val = redisTemplate.opsForValue().get(key);
        if (val == null) return null;
        if (type.isInstance(val)) return (T) val;
        return null;
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            redisTemplate.opsForValue().set(key, value);
        } else {
            redisTemplate.opsForValue().set(key, value, ttl);
        }
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean hasKey(String key) {
        Boolean b = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(b);
    }

    /* ============ Token 黑名单 ============ */

    @Override
    public void blacklistToken(String token, long remainMs) {
        if (token == null || token.isBlank() || remainMs <= 0) return;
        redisTemplate.opsForValue().set(KEY_TOKEN_BLACKLIST + token, "1", remainMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isBlank()) return false;
        Boolean b = redisTemplate.hasKey(KEY_TOKEN_BLACKLIST + token);
        return Boolean.TRUE.equals(b);
    }

    /* ============ 登录失败限流 ============ */

    @Override
    public long incrLoginFail(String phone, int maxFail, int lockSeconds) {
        if (phone == null || phone.isBlank()) return 0;
        String failKey = KEY_LOGIN_FAIL + phone;
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count == null) return 0;
        if (count == 1) {
            redisTemplate.expire(failKey, lockSeconds, TimeUnit.SECONDS);
        }
        if (count >= maxFail) {
            redisTemplate.opsForValue().set(KEY_LOGIN_LOCK + phone, "1", lockSeconds, TimeUnit.SECONDS);
        }
        return count;
    }

    @Override
    public void resetLoginFail(String phone) {
        if (phone == null || phone.isBlank()) return;
        redisTemplate.delete(KEY_LOGIN_FAIL + phone);
        redisTemplate.delete(KEY_LOGIN_LOCK + phone);
    }

    @Override
    public boolean isLoginLocked(String phone) {
        if (phone == null || phone.isBlank()) return false;
        Boolean b = redisTemplate.hasKey(KEY_LOGIN_LOCK + phone);
        return Boolean.TRUE.equals(b);
    }

    /* ============ 技能详情缓存 ============ */

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getSkillDetail(Long skillId, Class<T> type) {
        if (skillId == null) return null;
        Object val = redisTemplate.opsForValue().get(KEY_SKILL_DETAIL + skillId);
        if (val == null) return null;
        if (type.isInstance(val)) return (T) val;
        return null;
    }

    @Override
    public void putSkillDetail(Long skillId, Object detail, int ttlSeconds) {
        if (skillId == null || detail == null) return;
        if (ttlSeconds <= 0) {
            redisTemplate.opsForValue().set(KEY_SKILL_DETAIL + skillId, detail);
        } else {
            redisTemplate.opsForValue().set(KEY_SKILL_DETAIL + skillId, detail, ttlSeconds, TimeUnit.SECONDS);
        }
    }

    @Override
    public void evictSkillDetail(Long skillId) {
        if (skillId == null) return;
        redisTemplate.delete(KEY_SKILL_DETAIL + skillId);
    }

    /* ============ 技能浏览量 ============ */

    @Override
    public long incrSkillView(Long skillId) {
        if (skillId == null) return 0L;
        Long v = redisTemplate.opsForValue().increment(KEY_SKILL_VIEW + skillId);
        return v == null ? 0L : v;
    }

    @Override
    public Long getSkillView(Long skillId) {
        if (skillId == null) return null;
        Object v = redisTemplate.opsForValue().get(KEY_SKILL_VIEW + skillId);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
