package com.guessv.config;

import com.guessv.common.BizException;
import com.guessv.service.AdminService;
import com.guessv.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final AdminService adminService;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BizException(401, "未提供管理员凭证");
        }
        String token = auth.substring(7);
        if (!adminService.isValidAdminToken(token)) {
            throw new BizException(401, "管理员凭证无效");
        }
        Claims claims = jwtUtil.parse(token);
        req.setAttribute("adminUsername", claims.get("nickname"));
        return true;
    }
}
