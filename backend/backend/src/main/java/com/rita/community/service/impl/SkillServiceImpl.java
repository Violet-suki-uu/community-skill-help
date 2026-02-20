package com.rita.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rita.community.entity.Skill;
import com.rita.community.mapper.SkillMapper;
import com.rita.community.service.SkillService;
import org.springframework.stereotype.Service;

@Service
public class SkillServiceImpl implements SkillService {

    private final SkillMapper skillMapper;

    public SkillServiceImpl(SkillMapper skillMapper) {
        this.skillMapper = skillMapper;
    }

    @Override
    public Long create(Skill skill) {
        skillMapper.insert(skill);
        return skill.getId();
    }

    @Override
    public IPage<Skill> page(Page<Skill> page, String keyword, String category) {
        LambdaQueryWrapper<Skill> qw = new LambdaQueryWrapper<>();
        qw.eq(Skill::getStatus, 1);

        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Skill::getTitle, keyword).or().like(Skill::getDescription, keyword));
        }
        if (category != null && !category.isBlank()) {
            qw.eq(Skill::getCategory, category);
        }

        qw.orderByDesc(Skill::getCreatedAt);
        return skillMapper.selectPage(page, qw);
    }
}
