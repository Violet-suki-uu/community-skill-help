package com.rita.community.service;

import com.rita.community.dto.RecommendPageResp;

/**
 * RecommendService
 * 作用：推荐业务接口，定义推荐分页能力。
 */
public interface RecommendService {
    RecommendPageResp recommend(Long userId, String cursor, Integer size);
}


