package com.linjian.seckill.domain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linjian.seckill.domain.entity.SkUser;
import com.linjian.seckill.domain.mapper.SkUserMapper;
import com.linjian.seckill.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UserDetailsService 实现。
 * <p>
 * 放在 domain 而非 security：它需要访问 Mapper，而 security 不能依赖 domain。
 * security 模块只依赖 UserDetailsService 接口，实现由运行时注入。
 */
@Service
@RequiredArgsConstructor
public class SkUserDetailsService implements UserDetailsService {

    private final SkUserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SkUser user = userMapper.selectOne(
                Wrappers.<SkUser>lambdaQuery().eq(SkUser::getUsername, username));

        if (user == null) {
            // 统一提示"用户名或密码错误"，不暴露账号是否存在
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        return LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .role(user.getRole())
                .enabled(user.getStatus() != null && user.getStatus() == 1)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
                .build();
    }
}
