package com.rita.community.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}
