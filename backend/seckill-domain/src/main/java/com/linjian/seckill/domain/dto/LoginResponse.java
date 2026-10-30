package com.linjian.seckill.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;

    private String refreshToken;

    private Long userId;

    private String username;

    private String nickname;

    private String role;
}
