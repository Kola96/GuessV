package com.guessv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guessv.dto.ListJsonDto;
import com.guessv.entity.Vtuber;
import com.guessv.entity.VtuberGroup;
import com.guessv.mapper.VtuberGroupMapper;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportService {

    private final VtuberMapper vtuberMapper;
    private final VtuberGroupMapper groupMapper;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${app.data.list-json-path:../data/list.json}")
    private String defaultPath;

    @Value("${app.data.import-enabled:true}")
    private boolean importEnabled;

    @Transactional
    public void importIfEmpty() {
        if (!importEnabled) {
            log.info("数据导入已禁用");
            return;
        }
        if (vtuberMapper.selectCount(null) > 0) {
            log.info("VTuber 表非空，跳过导入");
            return;
        }
        importFromJsonInternal(defaultPath);
    }

    @Transactional
    public void importFromJson(String path) {
        importFromJsonInternal(path);
    }

    private void importFromJsonInternal(String path) {
        try {
            // 无协议前缀的相对路径按文件系统处理（classpath:/file: 前缀直接使用）
            String location = path.contains(":") ? path : "file:" + path;
            Resource resource = resourceLoader.getResource(location);
            try (InputStream is = resource.getInputStream()) {
                ListJsonDto dto = objectMapper.readValue(is, ListJsonDto.class);
                List<ListJsonDto.Vtb> vtbs = dto.getVtbs();

                // 过滤掉 bot 和非 vtuber 类型
                List<ListJsonDto.Vtb> valid = vtbs.stream()
                        .filter(v -> "vtuber".equals(v.getType()) && !v.isBot())
                        .toList();

                log.info("解析到 {} 条 VTuber（过滤后），原始 {} 条", valid.size(), vtbs.size());

                // 提取团体（去重，以 group uuid 为键）
                Map<String, VtuberGroup> groupCache = new HashMap<>();
                for (ListJsonDto.Vtb vtb : valid) {
                    if (vtb.getGroup() != null && !groupCache.containsKey(vtb.getGroup())) {
                        VtuberGroup g = new VtuberGroup();
                        g.setName(vtb.getGroupName() != null ? vtb.getGroupName() : "未知团体");
                        groupMapper.insert(g);
                        groupCache.put(vtb.getGroup(), g);
                    }
                }
                log.info("团体导入完成，共 {} 个", groupCache.size());

                // 插入 VTuber
                int success = 0;
                for (ListJsonDto.Vtb vtb : valid) {
                    try {
                        Vtuber v = new Vtuber();
                        v.setUuid(vtb.getUuid());
                        if (vtb.getName() != null) {
                            v.setNameCn(vtb.getName().getCn());
                            v.setNameEn(vtb.getName().getEn());
                            v.setNameJp(vtb.getName().getJp());
                            v.setNameDefault(vtb.getName().getDefaultLang());
                            v.setAliases(vtb.getName().getExtra() != null ? vtb.getName().getExtra() : List.of());
                        }

                        // 从 accounts 提取平台
                        List<String> platforms = vtb.getAccounts() == null ? List.of() :
                                vtb.getAccounts().stream()
                                        .map(ListJsonDto.Account::getPlatform)
                                        .filter(Objects::nonNull)
                                        .distinct()
                                        .toList();
                        v.setPlatforms(platforms);

                        VtuberGroup group = vtb.getGroup() != null ? groupCache.get(vtb.getGroup()) : null;
                        v.setGroupId(group != null ? group.getId() : null);
                        v.setGroupName(vtb.getGroupName());

                        v.setLockedFields(List.of());
                        v.setDataStatus("raw");
                        v.setDataSource("import");
                        v.setCreatedAt(LocalDateTime.now());
                        v.setUpdatedAt(LocalDateTime.now());

                        vtuberMapper.insert(v);
                        success++;
                    } catch (Exception e) {
                        log.warn("导入 {} 失败：{}", vtb.getUuid(), e.getMessage());
                    }
                }
                log.info("导入完成，成功 {} 条", success);
            }
        } catch (Exception e) {
            log.error("数据导入失败：{}", e.getMessage(), e);
            throw new RuntimeException("数据导入失败", e);
        }
    }
}
