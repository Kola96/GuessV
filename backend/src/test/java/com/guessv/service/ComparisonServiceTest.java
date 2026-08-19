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

    private Vtuber v(String name, String region, String group, Integer year,
                     String birthday, String gender, String status,
                     List<String> hair, Integer followers) {
        Vtuber v = new Vtuber();
        v.setNameEn(name); v.setNameCn(name); v.setNameDefault("en");
        v.setRegion(region); v.setGroupName(group); v.setDebutYear(year);
        v.setBirthday(birthday);
        v.setGender(gender); v.setActivityStatus(status);
        v.setHairColor(hair); v.setFollowerCount(followers);
        return v;
    }

    @Test
    void allExactMatch() {
        Vtuber g = v("Gura", "英语圈", "Hololive EN", 2020, "06-20",
                "female", "active", List.of("蓝"), 4400000);
        ComparisonResult r = comparisonService.compare(g, g);
        assertEquals("exact", r.region().match());
        assertEquals("exact", r.group().match());
        assertEquals("exact", r.debutYear().match());
        assertEquals("exact", r.birthday().match());
        assertEquals("exact", r.gender().match());
        assertEquals("exact", r.status().match());
        assertEquals("exact", r.hairColor().match());
        assertEquals("exact", r.followerCount().match());
    }

    @Test
    void groupPartialSameCompany() {
        Vtuber guess = v("A", "日本", "Hololive", 2018, "01-01",
                "female", "active", List.of("白"), 100000);
        Vtuber target = v("B", "日本", "Hololive EN", 2020, "02-02",
                "female", "active", List.of("白"), 200000);
        assertEquals("partial", comparisonService.compare(guess, target).group().match());
    }

    @Test
    void groupNoneDifferentCompany() {
        Vtuber guess = v("A", "日本", "Hololive", 2018, "01-01",
                "female", "active", List.of("白"), 100000);
        Vtuber target = v("B", "日本", "Nijisanji", 2018, "02-02",
                "female", "active", List.of("白"), 200000);
        assertEquals("none", comparisonService.compare(guess, target).group().match());
    }

    @Test
    void debutYearHigherWhenTargetLater() {
        Vtuber guess = v("A", "日", "G", 2018, "01-01",
                "female", "active", List.of("白"), 100000);
        Vtuber target = v("B", "日", "G", 2020, "02-02",
                "female", "active", List.of("白"), 200000);
        var f = comparisonService.compare(guess, target).debutYear();
        assertEquals("higher", f.match());
        assertEquals("↑", f.direction());
    }

    @Test
    void debutYearLowerWhenTargetEarlier() {
        Vtuber guess = v("A", "日", "G", 2020, "01-01",
                "female", "active", List.of("白"), 100000);
        Vtuber target = v("B", "日", "G", 2018, "02-02",
                "female", "active", List.of("白"), 200000);
        var f = comparisonService.compare(guess, target).debutYear();
        assertEquals("lower", f.match());
        assertEquals("↓", f.direction());
    }

    @Test
    void birthdayExactMatch() {
        Vtuber guess = v("A", "日", "G", 2020, "06-20",
                "female", "active", List.of("白"), 100000);
        Vtuber target = v("B", "日", "G", 2020, "06-20",
                "female", "active", List.of("白"), 200000);
        assertEquals("exact", comparisonService.compare(guess, target).birthday().match());
    }

    @Test
    void birthdayPartialSameMonth() {
        Vtuber guess = v("A", "日", "G", 2020, "06-20",
                "female", "active", List.of("白"), 100000);
        Vtuber target = v("B", "日", "G", 2020, "06-15",
                "female", "active", List.of("白"), 200000);
        assertEquals("partial", comparisonService.compare(guess, target).birthday().match());
    }

    @Test
    void birthdayNoneDifferentMonth() {
        Vtuber guess = v("A", "日", "G", 2020, "06-20",
                "female", "active", List.of("白"), 100000);
        Vtuber target = v("B", "日", "G", 2020, "07-20",
                "female", "active", List.of("白"), 200000);
        assertEquals("none", comparisonService.compare(guess, target).birthday().match());
    }

    @Test
    void followerCountHigherWhenTargetMore() {
        Vtuber guess = v("A", "日", "G", 2020, "01-01",
                "female", "active", List.of("白"), 100000);
        Vtuber target = v("B", "日", "G", 2020, "02-02",
                "female", "active", List.of("白"), 2000000);
        var f = comparisonService.compare(guess, target).followerCount();
        assertEquals("higher", f.match());
        assertEquals("↑", f.direction());
    }

    @Test
    void followerCountLowerWhenTargetLess() {
        Vtuber guess = v("A", "日", "G", 2020, "01-01",
                "female", "active", List.of("白"), 2000000);
        Vtuber target = v("B", "日", "G", 2020, "02-02",
                "female", "active", List.of("白"), 100000);
        var f = comparisonService.compare(guess, target).followerCount();
        assertEquals("lower", f.match());
        assertEquals("↓", f.direction());
    }

    @Test
    void followerCountPartialWhenClose() {
        Vtuber guess = v("A", "日", "G", 2020, "01-01",
                "female", "active", List.of("白"), 1000000);
        Vtuber target = v("B", "日", "G", 2020, "02-02",
                "female", "active", List.of("白"), 1050000);
        // 差距 5%，在 10% 以内 → partial
        assertEquals("partial", comparisonService.compare(guess, target).followerCount().match());
    }

    @Test
    void hairColorPartialOverlap() {
        Vtuber guess = v("A", "日", "G", 2020, "01-01",
                "female", "active", List.of("蓝"), 100000);
        Vtuber target = v("B", "日", "G", 2020, "02-02",
                "female", "active", List.of("蓝", "白"), 200000);
        assertEquals("partial", comparisonService.compare(guess, target).hairColor().match());
    }

    @Test
    void hairColorNoneNoOverlap() {
        Vtuber guess = v("A", "日", "G", 2020, "01-01",
                "female", "active", List.of("红"), 100000);
        Vtuber target = v("B", "日", "G", 2020, "02-02",
                "female", "active", List.of("蓝", "白"), 200000);
        assertEquals("none", comparisonService.compare(guess, target).hairColor().match());
    }

    @Test
    void nullFieldsBothNullCountsAsExact() {
        Vtuber guess = v("A", null, null, null, null, null, null, null, null);
        Vtuber target = v("B", null, null, null, null, null, null, null, null);
        ComparisonResult r = comparisonService.compare(guess, target);
        assertEquals("exact", r.region().match());
        assertEquals("exact", r.debutYear().match());
        assertEquals("exact", r.hairColor().match());
        assertEquals("exact", r.birthday().match());
        assertEquals("exact", r.followerCount().match());
    }

    @Test
    void statusTranslatedToChinese() {
        Vtuber g = v("A", "日", "G", 2020, "01-01",
                "female", "active", List.of("白"), 100000);
        Vtuber t = v("B", "日", "G", 2020, "02-02",
                "female", "graduated", List.of("白"), 200000);
        var r = comparisonService.compare(g, t);
        assertEquals("活动", r.status().value());
        assertEquals("毕业", comparisonService.compare(t, g).status().value());
    }
}
