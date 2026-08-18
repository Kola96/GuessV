package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room_player")
public class RoomPlayer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long userId;
    private String playerName;
    private Boolean isReady;
    private Integer score;
    private Integer finishRank;
    private Integer attemptsUsed;
    private Boolean isWinner;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
