package com.guessv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guessv.common.BizException;
import com.guessv.common.Result;
import com.guessv.entity.OperationLog;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.OperationLogMapper;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/vtuber")
@RequiredArgsConstructor
public class AdminVtuberController {

    private final VtuberMapper vtuberMapper;
    private final OperationLogMapper operationLogMapper;

    // 列表（分页 + 状态筛选 + 搜索）
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        QueryWrapper<Vtuber> qw = new QueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq("data_status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like("name_cn", keyword)
                    .or().like("name_en", keyword)
                    .or().like("name_jp", keyword));
        }
        qw.orderByDesc("updated_at");

        Page<Vtuber> p = vtuberMapper.selectPage(new Page<>(page, size), qw);
        return Result.ok(Map.of(
                "records", p.getRecords(),
                "total", p.getTotal(),
                "page", page,
                "size", size
        ));
    }

    // 详情
    @GetMapping("/{id}")
    public Result<Vtuber> detail(@PathVariable Long id) {
        Vtuber v = vtuberMapper.selectById(id);
        if (v == null) throw new BizException(404, "VTuber 不存在");
        return Result.ok(v);
    }

    // 编辑（自动锁定修改的字段）
    @PutMapping("/{id}/edit")
    public Result<Vtuber> edit(@PathVariable Long id, @RequestBody EditRequest req) {
        Vtuber v = vtuberMapper.selectById(id);
        if (v == null) throw new BizException(404, "VTuber 不存在");

        List<String> locked = v.getLockedFields() != null ? new java.util.ArrayList<>(v.getLockedFields()) : new java.util.ArrayList<>();

        for (Map.Entry<String, Object> entry : req.fields().entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            // 反射设置字段值
            try {
                var fieldObj = Vtuber.class.getDeclaredField(field);
                fieldObj.setAccessible(true);
                fieldObj.set(v, value);
            } catch (Exception e) {
                throw new BizException(400, "字段不合法: " + field);
            }
            // 自动锁定
            if (Boolean.TRUE.equals(req.lockFields()) && !locked.contains(field)) {
                locked.add(field);
            }
            // 记录操作日志
            OperationLog log = new OperationLog();
            log.setOperationType("edit_vtuber");
            log.setTargetType("vtuber");
            log.setTargetId(id);
            log.setFieldName(field);
            log.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(log);
        }
        v.setLockedFields(locked);
        v.setDataSource("manual");
        v.setUpdatedAt(LocalDateTime.now());
        vtuberMapper.updateById(v);
        return Result.ok(v);
    }

    // 状态流转
    @PostMapping("/{id}/promote")
    public Result<Vtuber> promote(@PathVariable Long id, @RequestBody PromoteRequest req) {
        Vtuber v = vtuberMapper.selectById(id);
        if (v == null) throw new BizException(404, "VTuber 不存在");
        v.setDataStatus(req.targetStatus());
        v.setUpdatedAt(LocalDateTime.now());
        vtuberMapper.updateById(v);

        OperationLog log = new OperationLog();
        log.setOperationType("promote_vtuber");
        log.setTargetType("vtuber");
        log.setTargetId(id);
        log.setNewValue(req.targetStatus());
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);

        return Result.ok(v);
    }

    // 解锁字段
    @PostMapping("/{id}/unlock")
    public Result<Vtuber> unlock(@PathVariable Long id, @RequestBody UnlockRequest req) {
        Vtuber v = vtuberMapper.selectById(id);
        if (v == null) throw new BizException(404, "VTuber 不存在");
        List<String> locked = v.getLockedFields() != null ? new java.util.ArrayList<>(v.getLockedFields()) : new java.util.ArrayList<>();
        locked.removeAll(req.fields());
        v.setLockedFields(locked);
        vtuberMapper.updateById(v);
        return Result.ok(v);
    }

    public record EditRequest(Map<String, Object> fields, Boolean lockFields) {}
    public record PromoteRequest(String targetStatus) {}
    public record UnlockRequest(List<String> fields) {}
}
