package com.rita.community.service;

import java.time.Duration;

/**
 * CacheService
 * 作用：缓存门面，集中封装项目中所有 Redis 业务用法（Token 黑名单、登录限流、技能缓存、浏览计数等）。
 */
public interface CacheService {

    /* ============ 通用 ============ */
    <T> T get(String key, Class<T> type);
    void set(String key, Object value, Duration ttl);
    void delete(String key);
    boolean hasKey(String key);

    /* ============ Token 黑名单 ============ */
    /**
     * 拉黑 Token，直到原 Token 自身过期为止。
     * @param token 原始 JWT 字符串
     * @param remainMs 距离过期剩余毫秒（<=0 时直接忽略）
     */
    void blacklistToken(String token, long remainMs);
    boolean isTokenBlacklisted(String token);

    /* ============ 登录失败限流 ============ */
    /**
     * @return 自增后的失败次数；若已被锁定则返回 -1
     */
    long incrLoginFail(String phone, int maxFail, int lockSeconds);
    void resetLoginFail(String phone);
    boolean isLoginLocked(String phone);

    /* ============ 技能详情缓存 ============ */
    <T> T getSkillDetail(Long skillId, Class<T> type);
    void putSkillDetail(Long skillId, Object detail, int ttlSeconds);
    void evictSkillDetail(Long skillId);

    /* ============ 技能浏览量（Redis 计数，异步/懒回写） ============ */
    long incrSkillView(Long skillId);
    Long getSkillView(Long skillId);
}
