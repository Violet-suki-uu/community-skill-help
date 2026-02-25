package com.rita.community.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Skill
 * 作用：数据库实体，与数据表字段一一映射，供 ORM 持久化使用。
 */
@Data
@TableName("skill")
public class Skill {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer status; // 1上架 0下架
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String imageUrl;
    private BigDecimal lng;
    private BigDecimal lat;
    private String address;
    private String adcode;
    @TableField("city_name")
    private String cityName;
    @TableField("view_count")
    private Integer viewCount;
}

