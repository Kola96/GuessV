package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "pool_tag", autoResultMap = true)
public class PoolTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tagName;
    private String description;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object filterRule;
    private Boolean isActive;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
