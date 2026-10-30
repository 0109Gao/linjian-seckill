package com.linjian.seckill.domain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linjian.seckill.common.BizException;
import com.linjian.seckill.common.ErrorCode;
import com.linjian.seckill.domain.dto.LoginRequest;
import com.linjian.seckill.domain.dto.LoginResponse;
import com.linjian.seckill.domain.dto.RegisterRequest;
import com.linjian.seckill.domain.entity.SkUser;
import com.linjian.seckill.domain.mapper.SkUserMapper;
import com.linjian.seckill.security.JwtUtil;
import com.linjian.seckill.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务：注册、登录、登出、刷新令牌。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Refresh Token 在 Redis 中的键前缀，与秒杀业务的 sk: 前缀区分开 */
    private static final String REFRESH_TOKEN_KEY = "sk:refresh:";

    private final AuthenticationManager authenticationManager;
    private final SkUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    /**
     * 注册：C 端用户固定为 USER 角色，管理员只能由后台或 SQL 初始化。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        Long exists = userMapper.selectCount(
                Wrappers.<SkUser>lambdaQuery().eq(SkUser::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }

        SkUser user = new SkUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname() : request.getUsername());
        user.setPhone(request.getPhone());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        log.info("新用户注册成功 userId={} username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    /**
     * 登录：交给 AuthenticationManager 校验密码，成功后签发双 Token。
     */
    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException e) {
            // 不区分"用户不存在"与"密码错误"，防止账号枚举
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        if (!loginUser.isEnabled()) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        String accessToken = jwtUtil.generateAccessToken(loginUser.getUserId(), loginUser.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(loginUser.getUserId(), loginUser.getUsername());

        // Refresh Token 入库到 Redis，登出与刷新时以 Redis 为准，避免旧 token 被复用
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_KEY + loginUser.getUserId(),
                refreshToken,
                jwtUtil.getRefreshTokenExpirationSeconds(),
                TimeUnit.SECONDS);

        log.info("用户登录成功 userId={} username={}", loginUser.getUserId(), loginUser.getUsername());

        return new LoginResponse(accessToken, refreshToken,
                loginUser.getUserId(), loginUser.getUsername(), loginUser.getNickname(), loginUser.getRole());
    }

    /**
     * 登出：删除 Redis 中的 Refresh Token。
     * <p>
     * 已知限制：Access Token 在有效期内仍然可用，若要立即失效需要维护黑名单，
     * 本项目采用"短有效期 Access Token"方案，不引入黑名单以保持链路简单。
     */
    public void logout(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_KEY + userId);
        log.info("用户登出 userId={}", userId);
    }

    /**
     * 刷新 Access Token。
     */
    public LoginResponse refresh(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Refresh Token 无效或已过期");
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        String cached = redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY + userId);
        if (cached == null || !cached.equals(refreshToken)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Refresh Token 已失效，请重新登录");
        }

        SkUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在或已被禁用");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        return new LoginResponse(newAccessToken, refreshToken,
                user.getId(), user.getUsername(), user.getNickname(), user.getRole());
    }

    /**
     * 查询当前用户信息（脱敏，不含密码）。
     */
    public SkUser currentUser(Long userId) {
        SkUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 初始化管理员账号（仅在库中无管理员时调用，供启动引导使用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void initAdminIfAbsent(String username, String rawPassword) {
        Long exists = userMapper.selectCount(
                Wrappers.<SkUser>lambdaQuery().eq(SkUser::getRole, "ADMIN"));
        if (exists != null && exists > 0) {
            return;
        }
        SkUser admin = new SkUser();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setNickname("系统管理员");
        admin.setRole("ADMIN");
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        userMapper.insert(admin);
        log.warn("已创建初始管理员账号 username={}，请登录后立即修改密码", username);
    }
}
