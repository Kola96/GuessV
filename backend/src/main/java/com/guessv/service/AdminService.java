package com.guessv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guessv.entity.Admin;
import com.guessv.mapper.AdminMapper;
import com.guessv.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService implements ApplicationRunner {

    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.admin.default-password:admin123}")
    private String defaultPassword;

    @Override
    public void run(ApplicationArguments args) {
        long count = adminMapper.selectCount(null);
        if (count == 0) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
            admin.setRole("admin");
            adminMapper.insert(admin);
            log.info("管理员账号已初始化：admin / {}", defaultPassword);
        }
    }

    public String login(String username, String password) {
        Admin admin = adminMapper.selectOne(
                new QueryWrapper<Admin>().eq("username", username));
        if (admin == null || !passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new com.guessv.common.BizException(401, "用户名或密码错误");
        }
        return jwtUtil.generate("admin:" + admin.getId(), admin.getUsername(), "", false);
    }

    public boolean isValidAdminToken(String token) {
        if (!jwtUtil.isValid(token)) return false;
        var claims = jwtUtil.parse(token);
        return claims.getSubject().startsWith("admin:");
    }
}
