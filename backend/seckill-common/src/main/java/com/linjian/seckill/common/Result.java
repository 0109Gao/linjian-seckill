package com.linjian.seckill.common;

import lombok.Data;
import org.slf4j.MDC;

/**
 * 统一响应结构。
 * <p>
 * 成功码沿用现有两个项目的 200（而非 0），保持个人项目之间的一致性；
 * traceId 由 TraceIdFilter 写入 MDC，异常排查时与日志一一对应。
 */
@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;
    private String traceId;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = MDC.get(TraceIdFilter.MDC_KEY);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), "success", null);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
