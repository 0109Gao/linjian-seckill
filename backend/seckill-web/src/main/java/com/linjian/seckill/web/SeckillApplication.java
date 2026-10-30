package com.linjian.seckill.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 林间秒杀系统启动类。
 * <p>
 * 组件扫描范围提升到 com.linjian.seckill，以覆盖 common / security / domain 三个模块；
 * Mapper 单独扫描 domain 下的 mapper 包。
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.linjian.seckill")
@MapperScan("com.linjian.seckill.domain.mapper")
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
