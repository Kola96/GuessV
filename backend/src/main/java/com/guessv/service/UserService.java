package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.common.BizException;
import com.guessv.dto.NicknameCheckResponse;
import com.guessv.dto.UserInitResponse;
import com.guessv.dto.UserProfileVO;
import com.guessv.entity.User;
import com.guessv.mapper.UserMapper;
import com.guessv.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String GAME_ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int GAME_ID_LENGTH = 4;
    private static final int GAME_ID_MAX_RETRY = 10;

    private final UserMapper userMapper;
    private final NicknameService nicknameService;
    private final JwtUtil jwtUtil;

    public UserInitResponse createAnonymousUser(String nickname, String deviceFingerprint) {
        if (nickname == null || nickname.isBlank()) {
            nickname = nicknameService.generateRandom();
        }
        var validation = nicknameService.validate(nickname);
        if (!validation.valid()) {
            String msg = switch (validation.reason()) {
                case "length" -> "昵称长度需为 2-16 字符";
                case "format" -> "昵称格式不合法（禁止使用 #）";
                case "sensitive" -> "昵称包含敏感词，请更换";
                default -> "昵称不合法";
            };
            throw new BizException(400, msg);
        }

        String gameId = generateUniqueGameId();

        User user = new User();
        user.setUuid(UUID.randomUUID().toString());
        user.setNickname(nickname);
        user.setGameId(gameId);
        user.setDeviceFingerprint(deviceFingerprint);
        user.setIsAnonymous(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastActiveAt(LocalDateTime.now());
        userMapper.insert(user);

        String token = jwtUtil.generate(user.getUuid(), nickname, gameId, true);

        log.info("创建匿名用户：{}#{}", nickname, gameId);
        return new UserInitResponse(
                user.getUuid(),
                nickname,
                gameId,
                nickname + "#" + gameId,
                token,
                true
        );
    }

    private String generateUniqueGameId() {
        for (int attempt = 0; attempt < GAME_ID_MAX_RETRY; attempt++) {
            StringBuilder sb = new StringBuilder(GAME_ID_LENGTH);
            for (int i = 0; i < GAME_ID_LENGTH; i++) {
                int idx = (int) (Math.random() * GAME_ID_CHARS.length());
                sb.append(GAME_ID_CHARS.charAt(idx));
            }
            String candidate = sb.toString();
            long exists = userMapper.selectCount(
                    new QueryWrapper<User>().eq("game_id", candidate));
            if (exists == 0) {
                return candidate;
            }
        }
        return GAME_ID_CHARS.charAt((int)(Math.random()*GAME_ID_CHARS.length()))
                + generateUniqueGameId();
    }

    public UserProfileVO getProfile(User user) {
        return new UserProfileVO(
                user.getUuid(),
                user.getNickname(),
                user.getGameId(),
                user.getNickname() + "#" + user.getGameId(),
                user.getIsAnonymous(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }

    public NicknameCheckResponse checkNickname(String nickname) {
        var v = nicknameService.validate(nickname);
        return new NicknameCheckResponse(v.valid(), v.reason());
    }

    @Transactional
    public UserProfileVO changeNickname(User user, String newNickname) {
        var v = nicknameService.validate(newNickname);
        if (!v.valid()) {
            String msg = switch (v.reason()) {
                case "length" -> "昵称长度需为 2-16 字符";
                case "format" -> "昵称格式不合法（禁止使用 #）";
                case "sensitive" -> "昵称包含敏感词，请更换";
                default -> "昵称不合法";
            };
            throw new BizException(400, msg);
        }
        user.setNickname(newNickname);
        userMapper.updateById(user);
        return getProfile(user);
    }
}
