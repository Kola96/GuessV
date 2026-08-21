package com.guessv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pool_item")
public class PoolItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long poolId;
    private Long vtuberId;
    private LocalDateTime createdAt;
}
