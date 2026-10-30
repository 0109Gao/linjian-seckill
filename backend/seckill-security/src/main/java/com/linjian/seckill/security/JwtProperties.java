package com.linjian.seckill.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置项，对应 application.yml 中的 jwt.*。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥，长度不足会导致 HMAC-SHA256 初始化失败 */
    private String secret;

    /** Access Token 有效期（毫秒） */
    private Long accessTokenExpiration = 7_200_000L;

    /** Refresh Token 有效期（毫秒） */
    private Long refreshTokenExpiration = 604_800_000L;

    /** 请求头名称 */
    private String header = "Authorization";

    /** Token 前缀（含尾部空格） */
    private String tokenPrefix = "Bearer ";

    /** 签发者标识 */
    private String issuer = "linjian-seckill";
}
