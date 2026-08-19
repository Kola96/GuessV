package com.guessv.service;

import com.guessv.dto.ComparisonResult;
import com.guessv.dto.FieldComparison;
import com.guessv.entity.Vtuber;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@Service
public class ComparisonService {

    public ComparisonResult compare(Vtuber guess, Vtuber target) {
        return new ComparisonResult(
                name(guess),
                compareString(guess.getRegion(), target.getRegion()),
                compareGroup(guess.getGroupName(), target.getGroupName()),
                compareYear(guess.getDebutYear(), target.getDebutYear()),
                compareString(guess.getGender(), target.getGender(), this::translateGender),
                compareString(guess.getActivityStatus(), target.getActivityStatus(), this::translateStatus),
                compareList(guess.getHairColor(), target.getHairColor()),
                compareString(guess.getFanName(), target.getFanName())
        );
    }

    private FieldComparison name(Vtuber v) {
        String display = "cn".equals(v.getNameDefault()) && v.getNameCn() != null
                ? v.getNameCn()
                : (v.getNameEn() != null ? v.getNameEn() : v.getNameCn());
        return new FieldComparison(display, "exact");
    }

    private FieldComparison compareString(String a, String b) {
        return compareString(a, b, s -> s);
    }

    private FieldComparison compareString(String a, String b, Function<String, String> translate) {
        String ta = a == null ? null : translate.apply(a);
        String tb = b == null ? null : translate.apply(b);
        if (ta == null && tb == null) return new FieldComparison(ta, "exact");
        if (ta == null || tb == null) return new FieldComparison(ta, "none");
        return new FieldComparison(ta, ta.equals(tb) ? "exact" : "none");
    }

    private FieldComparison compareGroup(String a, String b) {
        if (a == null && b == null) return new FieldComparison(null, "exact");
        if (a == null || b == null) return new FieldComparison(a, "none");
        if (a.equals(b)) return new FieldComparison(a, "exact");
        String headA = firstToken(a);
        String headB = firstToken(b);
        if (headA != null && headA.equalsIgnoreCase(headB)) {
            return new FieldComparison(a, "partial");
        }
        return new FieldComparison(a, "none");
    }

    private String firstToken(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        int sp = trimmed.indexOf(' ');
        return sp > 0 ? trimmed.substring(0, sp) : trimmed;
    }

    private FieldComparison compareYear(Integer a, Integer b) {
        if (a == null && b == null) return new FieldComparison(null, "exact");
        if (a == null || b == null) return new FieldComparison(a, "none");
        if (a.equals(b)) return new FieldComparison(a, "exact");
        if (a < b) return new FieldComparison(a, "higher", "↑");
        return new FieldComparison(a, "lower", "↓");
    }

    private FieldComparison compareList(Collection<String> a, Collection<String> b) {
        if (a == null && b == null) return new FieldComparison(null, "exact");
        if (a == null || b == null) return new FieldComparison(a, "none");
        Set<String> sa = new HashSet<>(a);
        Set<String> sb = new HashSet<>(b);
        if (sa.equals(sb)) return new FieldComparison(a, "exact");
        if (!Collections.disjoint(sa, sb)) return new FieldComparison(a, "partial");
        return new FieldComparison(a, "none");
    }

    private String translateGender(String g) {
        if (g == null) return null;
        return switch (g) {
            case "male" -> "男";
            case "female" -> "女";
            default -> "其他";
        };
    }

    private String translateStatus(String s) {
        if (s == null) return null;
        return switch (s) {
            case "active" -> "活动";
            case "graduated" -> "毕业";
            case "hiatus" -> "休止";
            case "suspended" -> "暂停";
            default -> s;
        };
    }
}
