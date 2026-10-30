package com.linjian.seckill.security;

import com.linjian.seckill.common.BizException;
import com.linjian.seckill.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：业务代码取当前登录用户的唯一入口。
 * <p>
 * 禁止在 Controller 里各自解析 token，否则一旦更换鉴权方式需要改动所有入口。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long getCurrentUserId() {
        return getLoginUser().getUserId();
    }

    public static String getCurrentUsername() {
        return getLoginUser().getUsername();
    }
}
