package com.rita.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rita.community.entity.Skill;

import java.util.List;

public interface SkillService {
    Long create(Skill skill);
    IPage<Skill> page(Page<Skill> page, String keyword, String category);
    Skill getById(Long id);
    List<Skill> listMine(Long userId);
    boolean updateStatus(Long userId, Long skillId, Integer status);
    boolean removeByOwner(Long userId, Long skillId);
}
