package com.linjian.seckill.common;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：所有异常统一转换为 Result，禁止把堆栈信息返回给前端。
 * <p>
 * 注意：AccessDeniedException 属于 Spring Security，本模块不依赖安全框架，
 * 该异常由 security 模块的 SecurityExceptionHandler 单独处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : ErrorCode.BAD_REQUEST.getMessage();
        return Result.error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        return Result.error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public Result<Void> handleBadRequest(Exception e) {
        log.warn("请求参数不合法：{}", e.getMessage());
        return Result.error(ErrorCode.BAD_REQUEST, "请求参数格式错误");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        return Result.error(ErrorCode.NOT_FOUND);
    }

    /**
     * 请求路径不存在（Spring 6 起静态资源/无匹配映射统一抛此异常）。
     * 属于客户端问题，不是系统故障：只记 debug 日志，返回 404 + 业务码 404，
     * 避免刷 ERROR 堆栈误导排查。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException e, HttpServletResponse response) {
        log.debug("请求路径不存在：{}", e.getResourcePath());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return Result.error(ErrorCode.NOT_FOUND);
    }

    /**
     * 兜底异常。注意：不把 e.getMessage() 直接返回前端，避免泄露内部实现细节。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletResponse response) {
        log.error("系统异常", e);
        return Result.error(ErrorCode.INTERNAL_ERROR);
    }
}
