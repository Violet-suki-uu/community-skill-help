package com.rita.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rita.community.entity.Skill;
import com.rita.community.mapper.SkillMapper;
import com.rita.community.service.SkillService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * SkillServiceImpl
 * 作用：技能业务实现，实现技能的增删改查与筛选分页。
 */
@Service
public class SkillServiceImpl implements SkillService {

    private final SkillMapper skillMapper;

    public SkillServiceImpl(SkillMapper skillMapper) {
        this.skillMapper = skillMapper;
    }

    @Override
    public Long create(Skill skill) {
        if (skill.getViewCount() == null) {
            skill.setViewCount(0);
        }
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
    public List<Skill> mine(Long userId) {
        LambdaQueryWrapper<Skill> qw = new LambdaQueryWrapper<>();
        qw.eq(Skill::getUserId, userId).orderByDesc(Skill::getCreatedAt);
        return skillMapper.selectList(qw);
    }

    @Override
    public boolean updateStatus(Long id, Long userId, Integer status) {
        LambdaQueryWrapper<Skill> qw = new LambdaQueryWrapper<>();
        qw.eq(Skill::getId, id).eq(Skill::getUserId, userId);
        Skill found = skillMapper.selectOne(qw);
        if (found == null) return false;

        if (Objects.equals(found.getStatus(), status)) {
            return true;
        }

        found.setStatus(status);
        return skillMapper.updateById(found) > 0;
    }

    @Override
    public boolean removeByOwner(Long id, Long userId) {
        LambdaQueryWrapper<Skill> qw = new LambdaQueryWrapper<>();
        qw.eq(Skill::getId, id).eq(Skill::getUserId, userId);
        return skillMapper.delete(qw) > 0;
    }

    @Override
    public boolean updateByOwner(Long id, Long userId, Skill update) {
        LambdaQueryWrapper<Skill> qw = new LambdaQueryWrapper<>();
        qw.eq(Skill::getId, id).eq(Skill::getUserId, userId);
        Skill found = skillMapper.selectOne(qw);
        if (found == null) return false;

        if (sameText(found.getTitle(), update.getTitle())
                && sameText(found.getDescription(), update.getDescription())
                && sameText(found.getCategory(), update.getCategory())
                && samePrice(found.getPrice(), update.getPrice())
                && sameText(found.getImageUrl(), update.getImageUrl())
                && samePrice(found.getLng(), update.getLng())
                && samePrice(found.getLat(), update.getLat())
                && sameText(found.getAddress(), update.getAddress())
                && sameText(found.getAdcode(), update.getAdcode())
                && sameText(found.getCityName(), update.getCityName())) {
            return true;
        }

        found.setTitle(update.getTitle());
        found.setDescription(update.getDescription());
        found.setCategory(update.getCategory());
        found.setPrice(update.getPrice());
        found.setImageUrl(update.getImageUrl());
        found.setLng(update.getLng());
        found.setLat(update.getLat());
        found.setAddress(update.getAddress());
        found.setAdcode(update.getAdcode());
        found.setCityName(update.getCityName());
        return skillMapper.updateById(found) > 0;
    }

    @Override
    public Skill getById(Long id) {
        return skillMapper.selectById(id);
    }

    @Override
    public void increaseViewCount(Long id) {
        if (id == null) return;
        UpdateWrapper<Skill> uw = new UpdateWrapper<>();
        uw.eq("id", id).setSql("view_count = IFNULL(view_count, 0) + 1");
        skillMapper.update(null, uw);
    }

    private boolean sameText(String a, String b) {
        return Objects.equals(a, b);
    }

    private boolean samePrice(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }
}

