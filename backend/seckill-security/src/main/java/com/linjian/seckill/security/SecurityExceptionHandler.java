package com.linjian.seckill.security;

import com.linjian.seckill.common.ErrorCode;
import com.linjian.seckill.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 安全相关异常处理。
 * <p>
 * 独立于 common 的 GlobalExceptionHandler：@Order 最高优先级，
 * 保证 @PreAuthorize 抛出的 AccessDeniedException 不会落到兜底的 Exception 分支，
 * 否则前端会拿到 5000 而不是明确的权限错误。
 * <p>
 * 返回 ResponseEntity 而不是 Result：HTTP 状态码必须与 SecurityConfig 里
 * 过滤器级别的处理保持一致（401/403）。否则同样是「没权限」，经过滤器拦截
 * 返回 403，经 @PreAuthorize 拦截却返回 200 只在 body 里写 403，前端基座的
 * Axios 拦截器会按状态码走两条不同的分支。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthentication(AuthenticationException e) {
        log.warn("认证失败：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(ErrorCode.UNAUTHORIZED));
    }
}
