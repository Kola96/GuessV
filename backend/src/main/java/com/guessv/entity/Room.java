package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "room", autoResultMap = true)
public class Room {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roomCode;
    private String status;
    private String gameMode;
    private Long targetId;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private Long winnerId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object config;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
