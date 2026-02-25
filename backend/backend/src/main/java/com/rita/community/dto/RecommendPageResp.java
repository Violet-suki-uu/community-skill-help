package com.rita.community.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RecommendPageResp
 * 作用：响应数据模型，把后端聚合后的字段返回给前端页面。
 */
@Data
public class RecommendPageResp {
    private List<SkillListItemResp> items = new ArrayList<>();
    private String nextCursor;
}


