package com.guessv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class NicknameService {

    private final ObjectMapper objectMapper;
    private List<String> adjectives = List.of();
    private List<String> nouns = List.of();
    private List<String> presets = List.of();
    private List<String> sensitiveWords = List.of();

    @PostConstruct
    void init() {
        try (InputStream is = new ClassPathResource("nicknames.json").getInputStream()) {
            JsonNode node = objectMapper.readTree(is);
            adjectives = toList(node.get("adjectives"));
            nouns = toList(node.get("nouns"));
            presets = toList(node.get("presets"));
        } catch (Exception e) {
            log.warn("加载昵称池失败: {}", e.getMessage());
        }
        try (InputStream is = new ClassPathResource("sensitive-words.txt").getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            sensitiveWords = content.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } catch (Exception e) {
            log.warn("加载敏感词库失败: {}", e.getMessage());
        }
        log.info("昵称池加载完成：形容词 {} / 名词 {} / 预设 {} / 敏感词 {}",
                adjectives.size(), nouns.size(), presets.size(), sensitiveWords.size());
    }

    private List<String> toList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> list = new ArrayList<>();
        node.forEach(n -> list.add(n.asText()));
        return list;
    }

    public String generateRandom() {
        Random r = ThreadLocalRandom.current();
        if (!presets.isEmpty() && r.nextBoolean()) {
            return presets.get(r.nextInt(presets.size()));
        }
        if (!adjectives.isEmpty() && !nouns.isEmpty()) {
            return adjectives.get(r.nextInt(adjectives.size()))
                    + nouns.get(r.nextInt(nouns.size()));
        }
        return "玩家" + r.nextInt(1000, 9999);
    }

    public boolean containsSensitive(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        for (String w : sensitiveWords) {
            if (lower.contains(w.toLowerCase())) return true;
        }
        return false;
    }

    public ValidationResult validate(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return new ValidationResult(false, "length");
        }
        int len = nickname.trim().length();
        if (len < 2 || len > 16) {
            return new ValidationResult(false, "length");
        }
        if (nickname.contains("#")) {
            return new ValidationResult(false, "format");
        }
        if (containsSensitive(nickname)) {
            return new ValidationResult(false, "sensitive");
        }
        return new ValidationResult(true, null);
    }

    public record ValidationResult(boolean valid, String reason) {}
}
