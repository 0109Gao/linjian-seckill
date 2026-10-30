package com.linjian.seckill.domain.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 库存专用 Redis 连接工厂的构造器。
 * <p>
 * 为什么这里只有静态方法、没有 {@code @Bean}：Spring Boot 的
 * {@code LettuceConnectionConfiguration} 用
 * {@code @ConditionalOnMissingBean(RedisConnectionFactory.class)} 注册默认工厂。
 * 一旦容器里出现任意一个 RedisConnectionFactory Bean，Boot 会**整体放弃**
 * 自动配置，于是 {@code StringRedisTemplate} 唯一可用的工厂就变成我们声明的
 * 那个（db1）。结果是登录 Token 这类本该进 db0 的数据会被静默写进 db1——
 * 这个坑本项目真实踩过一次，表现是 refreshToken 出现在库存库中。
 * <p>
 * 所以库存工厂只作为普通对象由 {@link com.linjian.seckill.domain.service.StockRedisService}
 * 自己持有并负责销毁，不参与容器装配。
 */
public final class StockRedisConnectionFactoryBuilder {

    private StockRedisConnectionFactoryBuilder() {
    }

    public static LettuceConnectionFactory build(RedisProperties properties, int database, Duration fallbackTimeout) {
        RedisStandaloneConfiguration standalone =
                new RedisStandaloneConfiguration(properties.getHost(), properties.getPort());
        standalone.setDatabase(database);
        if (StringUtils.hasText(properties.getPassword())) {
            standalone.setPassword(RedisPassword.of(properties.getPassword()));
        }

        Duration timeout = properties.getTimeout() == null ? fallbackTimeout : properties.getTimeout();
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(timeout)
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, clientConfiguration);
        // 手动创建的工厂不会自动初始化，必须显式调用，否则首次使用时才建连
        factory.afterPropertiesSet();
        return factory;
    }
}
