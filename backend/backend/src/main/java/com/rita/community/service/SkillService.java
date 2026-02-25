package com.rita.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rita.community.entity.Skill;

import java.util.List;

/**
 * SkillService
 * 作用：技能业务接口，定义技能相关核心业务能力。
 */
public interface SkillService {
    Long create(Skill skill);

    IPage<Skill> page(Page<Skill> page, String keyword, String category);

    List<Skill> mine(Long userId);

    boolean updateStatus(Long id, Long userId, Integer status);

    boolean removeByOwner(Long id, Long userId);

    boolean updateByOwner(Long id, Long userId, Skill update);

    Skill getById(Long id);

    void increaseViewCount(Long id);
}

