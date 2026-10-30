package com.linjian.seckill.web.initializer;

import com.linjian.seckill.domain.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动引导：库中不存在管理员时创建一个初始管理员。
 * <p>
 * 初始密码通过配置注入，默认值仅用于本地开发；生产部署必须用环境变量覆盖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AuthService authService;

    @Value("${seckill.init-admin.username:admin}")
    private String adminUsername;

    @Value("${seckill.init-admin.password:admin123456}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        try {
            authService.initAdminIfAbsent(adminUsername, adminPassword);
        } catch (Exception e) {
            // 初始化失败不应阻断应用启动（例如数据库尚未建表）
            log.warn("管理员初始化跳过：{}", e.getMessage());
        }
    }
}
