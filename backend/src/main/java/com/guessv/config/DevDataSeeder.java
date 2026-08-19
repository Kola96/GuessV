package com.guessv.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.entity.Vtuber;
import com.guessv.mapper.VtuberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 开发种子数据：插入 10 位完整属性的 active VTuber，用于游戏逻辑开发和前端联调。
 * 通过 app.data.seed-enabled 控制是否在启动时自动执行（test 环境关闭，避免污染测试库）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DevDataSeeder {

    private final VtuberMapper vtuberMapper;

    @Value("${app.data.seed-enabled:true}")
    private boolean seedEnabled;

    public void seedIfEnabled() {
        if (!seedEnabled) {
            return;
        }
        seed();
    }

    public void seed() {
        long activeCount = vtuberMapper.selectCount(
                new QueryWrapper<Vtuber>().eq("data_status", "active"));
        if (activeCount >= 15) {
            log.info("已有 {} 条 active 数据，跳过种子数据", activeCount);
            return;
        }

        List<Vtuber> seeds = List.of(
                build("seed-gura", "噶呜·古拉", "Gawr Gura", "がうる・ぐら", "cn",
                        2020, LocalDate.of(2020, 9, 13), "英语圈", "Hololive EN",
                        "active", "female", List.of("蓝", "白"), List.of("蓝"),
                        "Shrimp", "#1E90FF", List.of("YouTube", "Twitter", "Bilibili"), List.of("英语", "日语")),
                build("seed-calli", "森美声", "Mori Calliope", "森カリオペ", "cn",
                        2020, LocalDate.of(2020, 9, 12), "英语圈", "Hololive EN",
                        "active", "female", List.of("粉", "白"), List.of("红"),
                        "Dead Beats", "#C01A1A", List.of("YouTube", "Twitter"), List.of("英语", "日语")),
                build("seed-kiara", "小鸟游琪亚拉", "Takanashi Kiara", "小鳥遊キアラ", "cn",
                        2020, LocalDate.of(2020, 9, 12), "英语圈", "Hololive EN",
                        "active", "female", List.of("橙", "黄"), List.of("橙"),
                        "KFP", "#F9A01B", List.of("YouTube", "Twitter"), List.of("英语", "日语", "德语")),
                build("seed-ame", "亚美·华生", "Amelia Watson", "ワトソン・アメリア", "cn",
                        2020, LocalDate.of(2020, 9, 12), "英语圈", "Hololive EN",
                        "active", "female", List.of("黄"), List.of("蓝"),
                        "Teamates", "#FFE46B", List.of("YouTube", "Twitter"), List.of("英语")),
                build("seed-ina", "一伊那尓栖", "Ninomae Ina'nis", "ニノマエ・イナニス", "cn",
                        2020, LocalDate.of(2020, 9, 12), "英语圈", "Hololive EN",
                        "active", "female", List.of("紫", "黑"), List.of("紫"),
                        "Takodachi", "#6D5BB8", List.of("YouTube", "Twitter"), List.of("英语", "日语")),
                build("seed-fubuki", "白上吹雪", "Shirakami Fubuki", "白上フブキ", "cn",
                        2018, LocalDate.of(2018, 6, 1), "日本", "Hololive",
                        "active", "female", List.of("白"), List.of("蓝"),
                        "すく水部", "#00A0DC", List.of("YouTube", "Twitter", "Bilibili"), List.of("日语")),
                build("seed-pekora", "兔田佩克拉", "Usada Pekora", "兎田ぺこら", "cn",
                        2019, LocalDate.of(2019, 7, 17), "日本", "Hololive",
                        "active", "female", List.of("蓝"), List.of("橙"),
                        "野ウサギ", "#FF4500", List.of("YouTube", "Twitter"), List.of("日语")),
                build("seed-miko", "樱巫女", "Sakura Miko", "さくらみこ", "cn",
                        2018, LocalDate.of(2018, 8, 1), "日本", "Hololive",
                        "active", "female", List.of("粉"), List.of("绿"),
                        "35P", "#FF6B9D", List.of("YouTube", "Twitter", "Bilibili"), List.of("日语")),
                build("seed-aqua", "凑阿库娅", "Minato Aqua", "湊あくあ", "cn",
                        2018, LocalDate.of(2018, 8, 8), "日本", "Hololive",
                        "active", "female", List.of("紫"), List.of("紫"),
                        "Aqua Crew", "#B388FF", List.of("YouTube", "Twitter", "Bilibili"), List.of("日语")),
                build("seed-shion", "紫咲诗音", "Murasaki Shion", "紫咲シオン", "cn",
                        2018, LocalDate.of(2018, 8, 17), "日本", "Hololive",
                        "active", "female", List.of("紫"), List.of("橙"),
                        "塩っ子", "#9966CC", List.of("YouTube", "Twitter"), List.of("日语")),
                // ===== VirtuaReal 头部成员（数据来源：萌娘百科） =====
                build("seed-nana7mi", "七海Nana7mi", "Nana7mi", null, "cn",
                        2019, LocalDate.of(2019, 5, 11), "中国", "VirtuaReal",
                        "active", "female", List.of("棕"), List.of("金"),
                        "脆鲨", "#418BDE", List.of("bilibili", "YouTube"), List.of("汉语")),
                build("seed-guangyi", "中单光一", "Guangyi", null, "cn",
                        2019, LocalDate.of(2019, 5, 11), "中国", "VirtuaReal",
                        "active", "male", List.of("棕"), List.of("灰"),
                        "光一军", "#3B3B3B", List.of("bilibili"), List.of("汉语")),
                build("seed-xiaoke", "小可学妹", "Xiaoke", null, "cn",
                        2019, LocalDate.of(2019, 5, 18), "中国", "VirtuaReal",
                        "active", "female", List.of("黑"), List.of("红"),
                        "小可饼", "#FF6B6B", List.of("bilibili", "weibo"), List.of("汉语")),
                build("seed-miki", "弥希Miki", "Miki", null, "cn",
                        2020, LocalDate.of(2020, 2, 12), "中国", "VirtuaReal",
                        "active", "female", List.of("黑"), List.of("紫"),
                        "弥希家的", "#9D8DF1", List.of("bilibili"), List.of("汉语")),
                build("seed-sui", "岁己SUI", "SUI", null, "cn",
                        2022, LocalDate.of(2022, 8, 23), "中国", "VirtuaReal",
                        "active", "female", List.of("银"), List.of("红"),
                        "岁己家的", "#E8E8E8", List.of("bilibili"), List.of("汉语"))
        );

        int inserted = 0;
        for (Vtuber v : seeds) {
            long exists = vtuberMapper.selectCount(
                    new QueryWrapper<Vtuber>().eq("uuid", v.getUuid()));
            if (exists == 0) {
                vtuberMapper.insert(v);
                inserted++;
            }
        }
        log.info("种子数据已插入 {} 条", inserted);
    }

    private Vtuber build(String uuid, String nameCn, String nameEn, String nameJp, String nameDefault,
                         int debutYear, LocalDate debutDate, String region, String groupName,
                         String status, String gender, List<String> hairColor, List<String> eyeColor,
                         String fanName, String color, List<String> platforms, List<String> languages) {
        Vtuber v = new Vtuber();
        v.setUuid(uuid);
        v.setNameCn(nameCn);
        v.setNameEn(nameEn);
        v.setNameJp(nameJp);
        v.setNameDefault(nameDefault);
        v.setAliases(List.of());
        v.setDebutYear(debutYear);
        v.setDebutDate(debutDate);
        v.setRegion(region);
        v.setGroupName(groupName);
        v.setActivityStatus(status);
        v.setGender(gender);
        v.setHairColor(hairColor);
        v.setEyeColor(eyeColor);
        v.setFanName(fanName);
        v.setRepresentativeColor(color);
        v.setPlatforms(platforms);
        v.setLanguages(languages);
        v.setLockedFields(List.of());
        v.setDataStatus("active");
        v.setDataSource("manual");
        v.setCreatedAt(LocalDateTime.now());
        v.setUpdatedAt(LocalDateTime.now());
        return v;
    }
}
