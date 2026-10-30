package com.linjian.seckill.security;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * 登录用户上下文。
 * <p>
 * 刻意只保存原始字段而不引用实体类：security 模块不能反向依赖 domain，
 * 否则两个模块会形成循环依赖。
 */
@Getter
@Builder
public class LoginUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String nickname;
    private final String role;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
