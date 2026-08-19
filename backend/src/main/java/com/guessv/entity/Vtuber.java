package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "vtuber", autoResultMap = true)
public class Vtuber {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String uuid;
    private String nameCn;
    private String nameEn;
    private String nameJp;
    private String nameDefault;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> aliases;
    private Integer debutYear;
    private LocalDate debutDate;
    private String region;
    private Long groupId;
    private String groupName;
    private String activityStatus;
    private String gender;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> hairColor;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> eyeColor;
    private String outfitTheme;
    private String fanName;
    private String symbol;
    private String representativeColor;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> platforms;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> languages;
    private String avatarUrl;
    private String birthday;
    private Integer followerCount;
    private String dataStatus;
    private String dataSource;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> lockedFields;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
