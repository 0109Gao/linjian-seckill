package com.linjian.seckill.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置（springdoc + Knife4j）。
 * <p>
 * 访问地址：http://localhost:8081/doc.html
 * 在文档页右上角填入登录返回的 accessToken，即可调试需要鉴权的接口。
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Authorization";

    @Bean
    public OpenAPI seckillOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("林间秒杀系统 API")
                        .description("单体分模块架构 · Redis 预扣 + RabbitMQ 异步下单 + DB 乐观锁兜底")
                        .version("1.0.0")
                        .contact(new Contact().name("linjian")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name(SECURITY_SCHEME_NAME)));
    }
}
