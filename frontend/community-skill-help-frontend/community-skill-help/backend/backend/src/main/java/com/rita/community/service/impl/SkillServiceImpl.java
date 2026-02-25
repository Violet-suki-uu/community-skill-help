package com.rita.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rita.community.entity.Skill;
import com.rita.community.mapper.SkillMapper;
import com.rita.community.service.SkillService;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Override
    public Skill getById(Long id) {
        return skillMapper.selectById(id);
    }

    @Override
    public List<Skill> listMine(Long userId) {
        LambdaQueryWrapper<Skill> qw = new LambdaQueryWrapper<>();
        qw.eq(Skill::getUserId, userId);
        qw.orderByDesc(Skill::getCreatedAt);
        return skillMapper.selectList(qw);
    }

    @Override
    public boolean updateStatus(Long userId, Long skillId, Integer status) {
        LambdaUpdateWrapper<Skill> uw = new LambdaUpdateWrapper<>();
        uw.eq(Skill::getId, skillId)
          .eq(Skill::getUserId, userId)
          .set(Skill::getStatus, status);
        return skillMapper.update(null, uw) > 0;
    }

    @Override
    public boolean removeByOwner(Long userId, Long skillId) {
        LambdaQueryWrapper<Skill> qw = new LambdaQueryWrapper<>();
        qw.eq(Skill::getId, skillId)
          .eq(Skill::getUserId, userId);
        return skillMapper.delete(qw) > 0;
    }
}
