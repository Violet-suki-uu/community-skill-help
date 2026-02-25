package com.rita.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Reservation
 * 作用：数据库实体，与数据表字段一一映射，供 ORM 持久化使用。
 */
@Data
@TableName("reservation")
public class Reservation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("skill_id")
    private Long skillId;

    @TableField("buyer_id")
    private Long buyerId;

    @TableField("seller_id")
    private Long sellerId;

    private String status;

    private String address;

    private String phone;

    private String note;

    private Integer rating;

    @TableField("rating_comment")
    private String ratingComment;

    @TableField("rated_at")
    private LocalDateTime ratedAt;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

