package com.guessv.service;

import com.guessv.dto.ComparisonResult;
import com.guessv.entity.Vtuber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ComparisonServiceTest {

    @Autowired private ComparisonService comparisonService;

    private Vtuber v(String name, List<String> platforms, String group, Integer year,
                     String birthday, String gender, String status,
                     List<String> hair, List<String> langs, Integer followerBili, String market) {
        Vtuber v = new Vtuber();
        v.setNameEn(name); v.setNameCn(name); v.setNameDefault("en");
        v.setPlatforms(platforms); v.setGroupName(group); v.setDebutYear(year);
        v.setBirthday(birthday);
        v.setGender(gender); v.setActivityStatus(status);
        v.setHairColor(hair); v.setLanguages(langs);
        v.setFollowerBili(followerBili); v.setMarket(market);
        return v;
    }

    @Test
    void allExactMatch() {
        Vtuber g = v("Gura", List.of("YouTube", "Twitter"), "Hololive EN", 2020, "06-20",
                "female", "active", List.of("蓝"), List.of("英语"), 4400000, "both");
        ComparisonResult r = comparisonService.compare(g, g);
        assertEquals("exact", r.platforms().match());
        assertEquals("exact", r.group().match());
        assertEquals("exact", r.debutYear().match());
        assertEquals("exact", r.birthday().match());
        assertEquals("exact", r.gender().match());
        assertEquals("exact", r.status().match());
        assertEquals("exact", r.hairColor().match());
        assertEquals("exact", r.languages().match());
        assertEquals("exact", r.followerCount().match());
    }

    @Test
    void groupPartialSameCompany() {
        Vtuber guess = v("A", List.of("bilibili"), "Hololive", 2018, "01-01",
                "female", "active", List.of("白"), List.of("日语"), 100000, "cn");
        Vtuber target = v("B", List.of("bilibili"), "Hololive EN", 2020, "02-02",
                "female", "active", List.of("白"), List.of("英语"), 200000, "cn");
        assertEquals("partial", comparisonService.compare(guess, target).group().match());
    }

    @Test
    void platformsPartialOverlap() {
        Vtuber guess = v("A", List.of("YouTube", "Twitter"), "G", 2020, "01-01",
                "female", "active", List.of("白"), List.of("日语"), 100000, "both");
        Vtuber target = v("B", List.of("YouTube", "Bilibili"), "G", 2020, "02-02",
                "female", "active", List.of("白"), List.of("汉语"), 200000, "both");
        assertEquals("partial", comparisonService.compare(guess, target).platforms().match());
    }

    @Test
    void languagesPartialOverlap() {
        Vtuber guess = v("A", List.of("YouTube"), "G", 2020, "01-01",
                "female", "active", List.of("白"), List.of("英语", "日语"), 100000, "both");
        Vtuber target = v("B", List.of("YouTube"), "G", 2020, "02-02",
                "female", "active", List.of("白"), List.of("日语", "德语"), 200000, "both");
        assertEquals("partial", comparisonService.compare(guess, target).languages().match());
    }

    @Test
    void debutYearHigherWhenTargetLater() {
        Vtuber guess = v("A", List.of("bilibili"), "G", 2018, "01-01",
                "female", "active", List.of("白"), List.of("日语"), 100000, "cn");
        Vtuber target = v("B", List.of("bilibili"), "G", 2020, "02-02",
                "female", "active", List.of("白"), List.of("日语"), 200000, "cn");
        var f = comparisonService.compare(guess, target).debutYear();
        assertEquals("higher", f.match());
        assertEquals("↑", f.direction());
    }

    @Test
    void birthdayExactMatch() {
        Vtuber guess = v("A", List.of("bilibili"), "G", 2020, "06-20",
                "female", "active", List.of("白"), List.of("日语"), 100000, "cn");
        Vtuber target = v("B", List.of("bilibili"), "G", 2020, "06-20",
                "female", "active", List.of("白"), List.of("日语"), 200000, "cn");
        assertEquals("exact", comparisonService.compare(guess, target).birthday().match());
    }

    @Test
    void birthdayPartialSameMonth() {
        Vtuber guess = v("A", List.of("bilibili"), "G", 2020, "06-20",
                "female", "active", List.of("白"), List.of("日语"), 100000, "cn");
        Vtuber target = v("B", List.of("bilibili"), "G", 2020, "06-15",
                "female", "active", List.of("白"), List.of("日语"), 200000, "cn");
        assertEquals("partial", comparisonService.compare(guess, target).birthday().match());
    }

    @Test
    void followerCountHigherWhenTargetMore() {
        Vtuber guess = v("A", List.of("bilibili"), "G", 2020, "01-01",
                "female", "active", List.of("白"), List.of("日语"), 100000, "cn");
        Vtuber target = v("B", List.of("bilibili"), "G", 2020, "02-02",
                "female", "active", List.of("白"), List.of("日语"), 2000000, "cn");
        var f = comparisonService.compare(guess, target).followerCount();
        assertEquals("higher", f.match());
        assertEquals("↑", f.direction());
    }

    @Test
    void followerCountPartialWhenClose() {
        Vtuber guess = v("A", List.of("bilibili"), "G", 2020, "01-01",
                "female", "active", List.of("白"), List.of("日语"), 1000000, "cn");
        Vtuber target = v("B", List.of("bilibili"), "G", 2020, "02-02",
                "female", "active", List.of("白"), List.of("日语"), 1050000, "cn");
        assertEquals("partial", comparisonService.compare(guess, target).followerCount().match());
    }

    @Test
    void hairColorPartialOverlap() {
        Vtuber guess = v("A", List.of("bilibili"), "G", 2020, "01-01",
                "female", "active", List.of("蓝"), List.of("日语"), 100000, "cn");
        Vtuber target = v("B", List.of("bilibili"), "G", 2020, "02-02",
                "female", "active", List.of("蓝", "白"), List.of("日语"), 200000, "cn");
        assertEquals("partial", comparisonService.compare(guess, target).hairColor().match());
    }

    @Test
    void nullFieldsBothNullCountsAsExact() {
        Vtuber guess = v("A", null, null, null, null, null, null, null, null, null, null);
        Vtuber target = v("B", null, null, null, null, null, null, null, null, null, null);
        ComparisonResult r = comparisonService.compare(guess, target);
        assertEquals("exact", r.platforms().match());
        assertEquals("exact", r.debutYear().match());
        assertEquals("exact", r.hairColor().match());
        assertEquals("exact", r.languages().match());
        assertEquals("exact", r.followerCount().match());
    }

    @Test
    void statusTranslatedToChinese() {
        Vtuber g = v("A", List.of("bilibili"), "G", 2020, "01-01",
                "female", "active", List.of("白"), List.of("日语"), 100000, "cn");
        Vtuber t = v("B", List.of("bilibili"), "G", 2020, "02-02",
                "female", "graduated", List.of("白"), List.of("日语"), 200000, "cn");
        assertEquals("活跃中", comparisonService.compare(g, t).status().value());
        assertEquals("不活跃", comparisonService.compare(t, g).status().value());
    }
}
