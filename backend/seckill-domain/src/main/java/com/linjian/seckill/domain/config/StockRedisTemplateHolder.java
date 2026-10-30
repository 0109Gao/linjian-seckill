package com.linjian.seckill.domain.config;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 库存库（db1）的 Redis 模板持有者。
 * <p>
 * 为什么不直接把这个工厂注册成 Bean：Spring Boot 的
 * {@code LettuceConnectionConfiguration} 上有
 * {@code @ConditionalOnMissingBean(RedisConnectionFactory.class)}。一旦容器里
 * 出现第二个工厂，Boot 会自动放弃创建默认工厂，登录 Token 等所有走默认模板的
 * 业务都会被静默改到 db1 —— 这个故障不报错、只表现为「Token 换库了」，
 * 极难发现（W2 已踩过一次）。
 * <p>
 * 同理也不能把 {@link StringRedisTemplate} 暴露成 Bean：
 * {@code RedisAutoConfiguration} 上同样带
 * {@code @ConditionalOnMissingBean(name = "redisTemplate")} 与
 * {@code @ConditionalOnMissingBean(StringRedisTemplate.class)}。
 * <p>
 * 所以这里只注册自己，模板通过 {@link #template()} 交给需要的服务使用。
 */
@Component
public class StockRedisTemplateHolder {

    private final LettuceConnectionFactory connectionFactory;

    private final StringRedisTemplate template;

    public StockRedisTemplateHolder(RedisProperties properties,
                                    @Value("${seckill.redis.stock-database:1}") int stockDatabase) {
        this.connectionFactory = StockRedisConnectionFactoryBuilder.build(
                properties, stockDatabase, Duration.ofSeconds(3));
        this.template = new StringRedisTemplate(connectionFactory);
        this.template.afterPropertiesSet();
    }

    public StringRedisTemplate template() {
        return template;
    }

    /**
     * 释放 Lettuce 连接与 Netty 线程池。热部署或优雅停机时不做这一步会持续泄漏。
     */
    @PreDestroy
    public void destroy() {
        connectionFactory.destroy();
    }
}
