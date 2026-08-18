package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "game_record", autoResultMap = true)
public class GameRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String mode;
    private Long targetId;
    private String poolTag;
    private Integer attempts;
    private Integer maxAttempts;
    private Boolean isWin;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object guesses;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
