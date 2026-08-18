package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "crawl_log", autoResultMap = true)
public class CrawlLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long vtuberId;
    private String source;
    private String status;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object fieldsUpdated;
    private String errorMessage;
    private LocalDateTime createdAt;
}
