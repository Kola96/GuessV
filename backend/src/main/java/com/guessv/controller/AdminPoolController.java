package com.guessv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.common.BizException;
import com.guessv.common.Result;
import com.guessv.entity.Pool;
import com.guessv.entity.PoolItem;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.PoolItemMapper;
import com.guessv.mapper.PoolMapper;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/pool")
@RequiredArgsConstructor
public class AdminPoolController {

    private final PoolMapper poolMapper;
    private final PoolItemMapper poolItemMapper;
    private final VtuberMapper vtuberMapper;

    // ===== 题库 CRUD =====

    @GetMapping
    public Result<List<Pool>> list(
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String market) {
        QueryWrapper<Pool> qw = new QueryWrapper<Pool>().orderByAsc("sort_order");
        if (mode != null) qw.eq("mode", mode);
        if (market != null) qw.eq("market", market);
        return Result.ok(poolMapper.selectList(qw));
    }

    @PostMapping
    public Result<Pool> create(@RequestBody Pool pool) {
        if (pool.getName() == null || pool.getName().isBlank())
            throw new BizException(400, "题库名不能为空");
        if (pool.getMode() == null) pool.setMode("single");
        if (pool.getMarket() == null) pool.setMarket("cn");
        pool.setIsActive(true);
        poolMapper.insert(pool);
        return Result.ok(pool);
    }

    @PutMapping("/{id}")
    public Result<Pool> update(@PathVariable Long id, @RequestBody Pool pool) {
        Pool existing = poolMapper.selectById(id);
        if (existing == null) throw new BizException(404, "题库不存在");
        pool.setId(id);
        poolMapper.updateById(pool);
        return Result.ok(poolMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        poolMapper.deleteById(id);
        poolItemMapper.delete(new QueryWrapper<PoolItem>().eq("pool_id", id));
        return Result.ok();
    }

    // ===== 题库成员管理 =====

    @GetMapping("/{id}/items")
    public Result<List<Vtuber>> items(@PathVariable Long id) {
        List<PoolItem> items = poolItemMapper.selectList(
                new QueryWrapper<PoolItem>().eq("pool_id", id));
        if (items.isEmpty()) return Result.ok(List.of());
        List<Long> ids = items.stream().map(PoolItem::getVtuberId).toList();
        return Result.ok(vtuberMapper.selectBatchIds(ids));
    }

    @PostMapping("/{id}/items")
    public Result<Void> addItems(@PathVariable Long id, @RequestBody AddItemsRequest req) {
        for (Long vtuberId : req.vtuberIds()) {
            long exists = poolItemMapper.selectCount(
                    new QueryWrapper<PoolItem>().eq("pool_id", id).eq("vtuber_id", vtuberId));
            if (exists == 0) {
                PoolItem item = new PoolItem();
                item.setPoolId(id);
                item.setVtuberId(vtuberId);
                poolItemMapper.insert(item);
            }
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}/items/{vtuberId}")
    public Result<Void> removeItem(@PathVariable Long id, @PathVariable Long vtuberId) {
        poolItemMapper.delete(new QueryWrapper<PoolItem>()
                .eq("pool_id", id).eq("vtuber_id", vtuberId));
        return Result.ok();
    }

    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        long count = poolItemMapper.selectCount(new QueryWrapper<PoolItem>().eq("pool_id", id));
        return Result.ok(Map.of("itemCount", count));
    }

    public record AddItemsRequest(List<Long> vtuberIds) {}
}
