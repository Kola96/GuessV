package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("\"user\"")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String uuid;
    private String nickname;
    private String gameId;
    private String username;
    private String passwordHash;
    private String email;
    private String oauthProvider;
    private String oauthId;
    private String avatarUrl;
    private String deviceFingerprint;
    private Boolean isAnonymous;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
}
