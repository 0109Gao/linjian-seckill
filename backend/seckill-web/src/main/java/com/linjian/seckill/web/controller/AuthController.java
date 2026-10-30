package com.linjian.seckill.web.controller;

import com.linjian.seckill.common.Result;
import com.linjian.seckill.domain.dto.LoginRequest;
import com.linjian.seckill.domain.dto.LoginResponse;
import com.linjian.seckill.domain.dto.RefreshTokenRequest;
import com.linjian.seckill.domain.dto.RegisterRequest;
import com.linjian.seckill.domain.entity.SkUser;
import com.linjian.seckill.domain.service.AuthService;
import com.linjian.seckill.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 */
@Tag(name = "认证", description = "注册、登录、登出、刷新令牌")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册", description = "C 端用户注册，注册成功返回用户 ID")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "登录", description = "返回 accessToken 与 refreshToken")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "刷新令牌", description = "用 refreshToken 换取新的 accessToken")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refresh(request.getRefreshToken()));
    }

    @Operation(summary = "登出", description = "删除 Redis 中的 refreshToken")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(SecurityUtils.getCurrentUserId());
        return Result.success();
    }

    @Operation(summary = "当前用户信息", description = "返回脱敏后的用户信息，不含密码")
    @GetMapping("/me")
    public Result<SkUser> me() {
        return Result.success(authService.currentUser(SecurityUtils.getCurrentUserId()));
    }
}
