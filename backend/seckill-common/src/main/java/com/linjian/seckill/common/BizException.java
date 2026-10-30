package com.linjian.seckill.common;

import lombok.Getter;

/**
 * 业务异常。
 * <p>
 * 由 GlobalExceptionHandler 统一转换为 Result，业务代码无需 try-catch。
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
