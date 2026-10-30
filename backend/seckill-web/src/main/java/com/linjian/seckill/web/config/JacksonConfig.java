package com.linjian.seckill.web.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局序列化配置。
 * <p>
 * 1. 时间格式：application.yml 里的 spring.jackson.date-format 只对 java.util.Date 生效，
 *    对 LocalDateTime 无效，默认会输出 ISO-8601 的 "2026-10-30T12:32:36"。
 *    这里统一为 "yyyy-MM-dd HH:mm:ss"，与前端基座的表格列显示习惯一致。
 * <p>
 * 2. Long → String：雪花 ID 量级 1.1e17~2.1e18，远超 JS 的 Number.MAX_SAFE_INTEGER(9.007e15)。
 *    直接输出数字会被 JSON.parse 静默截断末位，导致：
 *    - 结果页拿失真 requestId 查询 → 永远「结果确认中」，实际订单已落库；
 *    - 后台订单表把 userId 2116113282478075906 显示成 2116113282478076000。
 *    统一序列化为字符串后，前端拿到的是精确值，且请求体回传字符串也能被 Jackson 正常反序列化为 Long。
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);

        return builder -> builder
                .serializers(new LocalDateTimeSerializer(dateTimeFormatter))
                .serializers(new LocalDateSerializer(dateFormatter))
                .deserializers(new LocalDateTimeDeserializer(dateTimeFormatter))
                .deserializers(new LocalDateDeserializer(dateFormatter))
                // 雪花 ID 精度保护：仅超出 JS 安全整数范围的 Long 输出为字符串，
                // 小数字（分页 total、库存、数量）保持数字类型，不影响前端计算。
                .serializerByType(Long.class, new LongSafeSerializer())
                .serializerByType(Long.TYPE, new LongSafeSerializer());
    }
}
