package com.linjian.seckill.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类：签发与解析 Access / Refresh 两类 Token。
 * <p>
 * 两类 Token 共用同一密钥，但通过 claim 中的 tokenType 区分，
 * 避免 Refresh Token 被当作 Access Token 直接访问业务接口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username) {
        return generateToken(userId, username, TYPE_ACCESS, properties.getAccessTokenExpiration());
    }

    public String generateRefreshToken(Long userId, String username) {
        return generateToken(userId, username, TYPE_REFRESH, properties.getRefreshTokenExpiration());
    }

    private String generateToken(Long userId, String username, String tokenType, Long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_TOKEN_TYPE, tokenType);

        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .issuer(properties.getIssuer())
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(getKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 解析并校验 token 类型。类型不匹配时返回 null，而不是抛异常，
     * 便于过滤器把「无效 token」统一当成未登录处理。
     */
    public Claims parseToken(String token, String expectedType) {
        try {
            Claims claims = parseToken(token);
            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!expectedType.equals(tokenType)) {
                log.debug("token 类型不匹配，期望 {} 实际 {}", expectedType, tokenType);
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("token 解析失败：{}", e.getMessage());
            return null;
        }
    }

    public Long getUserId(String token) {
        return parseToken(token).get(CLAIM_USER_ID, Long.class);
    }

    public String getUsername(String token) {
        return parseToken(token).get(CLAIM_USERNAME, String.class);
    }

    public boolean validateAccessToken(String token) {
        return parseToken(token, TYPE_ACCESS) != null;
    }

    public boolean validateRefreshToken(String token) {
        return parseToken(token, TYPE_REFRESH) != null;
    }

    /** Refresh Token 有效期（秒），供 Redis TTL 使用 */
    public long getRefreshTokenExpirationSeconds() {
        return properties.getRefreshTokenExpiration() / 1000;
    }

    /** Access Token 有效期（秒） */
    public long getAccessTokenExpirationSeconds() {
        return properties.getAccessTokenExpiration() / 1000;
    }

    public JwtProperties getProperties() {
        return properties;
    }
}
