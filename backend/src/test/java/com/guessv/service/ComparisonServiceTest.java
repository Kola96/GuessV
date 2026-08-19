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
                     String gender, String status, List<String> hair, String fan) {
        Vtuber v = new Vtuber();
        v.setNameEn(name); v.setNameCn(name); v.setNameDefault("en");
        v.setRegion(region); v.setGroupName(group); v.setDebutYear(year);
        v.setGender(gender); v.setActivityStatus(status);
        v.setHairColor(hair); v.setFanName(fan);
        return v;
    }

    @Test
    void allExactMatch() {
        Vtuber g = v("Gura", "英语圈", "Hololive EN", 2020, "female", "active", List.of("蓝"), "Shrimp");
        ComparisonResult r = comparisonService.compare(g, g);
        assertEquals("exact", r.region().match());
        assertEquals("exact", r.group().match());
        assertEquals("exact", r.debutYear().match());
        assertEquals("exact", r.gender().match());
        assertEquals("exact", r.status().match());
        assertEquals("exact", r.hairColor().match());
        assertEquals("exact", r.fanName().match());
    }

    @Test
    void groupPartialSameCompany() {
        Vtuber guess = v("A", "日本", "Hololive", 2018, "female", "active", List.of("白"), "F");
        Vtuber target = v("B", "日本", "Hololive EN", 2020, "female", "active", List.of("白"), "F");
        assertEquals("partial", comparisonService.compare(guess, target).group().match());
    }

    @Test
    void groupNoneDifferentCompany() {
        Vtuber guess = v("A", "日本", "Hololive", 2018, "female", "active", List.of("白"), "F");
        Vtuber target = v("B", "日本", "Nijisanji", 2018, "female", "active", List.of("白"), "F");
        assertEquals("none", comparisonService.compare(guess, target).group().match());
    }

    @Test
    void debutYearHigherWhenTargetLater() {
        Vtuber guess = v("A", "日", "G", 2018, "female", "active", List.of("白"), "F");
        Vtuber target = v("B", "日", "G", 2020, "female", "active", List.of("白"), "F");
        var f = comparisonService.compare(guess, target).debutYear();
        assertEquals("higher", f.match());
        assertEquals("↑", f.direction());
    }

    @Test
    void debutYearLowerWhenTargetEarlier() {
        Vtuber guess = v("A", "日", "G", 2020, "female", "active", List.of("白"), "F");
        Vtuber target = v("B", "日", "G", 2018, "female", "active", List.of("白"), "F");
        var f = comparisonService.compare(guess, target).debutYear();
        assertEquals("lower", f.match());
        assertEquals("↓", f.direction());
    }

    @Test
    void hairColorPartialOverlap() {
        Vtuber guess = v("A", "日", "G", 2020, "female", "active", List.of("蓝"), "F");
        Vtuber target = v("B", "日", "G", 2020, "female", "active", List.of("蓝", "白"), "F");
        assertEquals("partial", comparisonService.compare(guess, target).hairColor().match());
    }

    @Test
    void hairColorNoneNoOverlap() {
        Vtuber guess = v("A", "日", "G", 2020, "female", "active", List.of("红"), "F");
        Vtuber target = v("B", "日", "G", 2020, "female", "active", List.of("蓝", "白"), "F");
        assertEquals("none", comparisonService.compare(guess, target).hairColor().match());
    }

    @Test
    void nullFieldsBothNullCountsAsExact() {
        Vtuber guess = v("A", null, null, null, null, null, null, null);
        Vtuber target = v("B", null, null, null, null, null, null, null);
        ComparisonResult r = comparisonService.compare(guess, target);
        assertEquals("exact", r.region().match());
        assertEquals("exact", r.debutYear().match());
        assertEquals("exact", r.hairColor().match());
    }

    @Test
    void statusTranslatedToChinese() {
        Vtuber g = v("A", "日", "G", 2020, "female", "active", List.of("白"), "F");
        Vtuber t = v("B", "日", "G", 2020, "female", "graduated", List.of("白"), "F");
        var r = comparisonService.compare(g, t);
        assertEquals("活动", r.status().value());
        assertEquals("毕业", comparisonService.compare(t, g).status().value());
    }
}
