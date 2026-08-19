package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.common.BizException;
import com.guessv.dto.VtuberSearchVO;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VtuberService {

    private final VtuberMapper vtuberMapper;

    public List<VtuberSearchVO> search(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            throw new BizException(400, "搜索关键词不能为空");
        }
        if (limit <= 0 || limit > 20) {
            limit = 10;
        }

        String kw = "%" + keyword.trim() + "%";
        List<Vtuber> vtubers = vtuberMapper.selectList(
                new QueryWrapper<Vtuber>()
                        .in("data_status", "active", "verified")
                        .and(w -> w.like("name_cn", kw)
                                .or().like("name_en", kw)
                                .or().like("name_jp", kw))
                        .last("LIMIT " + limit));

        return vtubers.stream()
                .map(this::toVO)
                .toList();
    }

    private VtuberSearchVO toVO(Vtuber v) {
        String displayName;
        if ("cn".equals(v.getNameDefault())) {
            displayName = v.getNameCn() != null ? v.getNameCn()
                    : v.getNameEn() != null ? v.getNameEn() : v.getNameJp();
        } else {
            displayName = v.getNameEn() != null ? v.getNameEn()
                    : v.getNameCn() != null ? v.getNameCn() : v.getNameJp();
        }
        return new VtuberSearchVO(
                v.getId(),
                displayName,
                v.getNameCn(),
                v.getNameEn(),
                v.getAvatarUrl(),
                v.getGroupName(),
                v.getRegion()
        );
    }
}
