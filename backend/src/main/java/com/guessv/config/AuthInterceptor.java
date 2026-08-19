package com.guessv.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.common.BizException;
import com.guessv.entity.User;
import com.guessv.mapper.UserMapper;
import com.guessv.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_CURRENT_USER = "currentUser";

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        String token = req.getHeader("X-User-Token");
        if (token == null || token.isBlank()) {
            throw new BizException(401, "未提供用户凭证");
        }
        if (!jwtUtil.isValid(token)) {
            throw new BizException(401, "用户凭证无效或已过期");
        }
        Claims claims = jwtUtil.parse(token);
        String userId = claims.getSubject();
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("uuid", userId));
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        req.setAttribute(ATTR_USER_ID, user.getUuid());
        req.setAttribute(ATTR_CURRENT_USER, user);
        try {
            user.setLastActiveAt(LocalDateTime.now());
            userMapper.updateById(user);
        } catch (Exception ignored) {}
        return true;
    }
}
