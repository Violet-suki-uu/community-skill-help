package com.rita.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rita.community.entity.Skill;

public interface SkillService {
    Long create(Skill skill);
    IPage<Skill> page(Page<Skill> page, String keyword, String category);
}
