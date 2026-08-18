package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_target")
public class DailyTarget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate targetDate;
    private Long vtuberId;
    private LocalDateTime createdAt;
}
