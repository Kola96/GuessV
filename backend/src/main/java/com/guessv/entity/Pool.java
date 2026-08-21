package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pool")
public class Pool {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String market;
    private String mode;
    private Boolean isActive;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
