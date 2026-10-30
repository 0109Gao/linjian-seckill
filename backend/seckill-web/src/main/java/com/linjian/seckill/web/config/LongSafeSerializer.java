package com.linjian.seckill.web.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Long 精度安全序列化器。
 * <p>
 * 雪花 ID（1.1e17 ~ 2.1e18）超出 JS 的 Number.MAX_SAFE_INTEGER(9007199254740991)，
 * 直接输出数字会被 JSON.parse 静默改写末位，导致前端拿到的 ID 与后端不一致
 * （实测：订单 requestId 后端 109597783211638784，前端变 109597783211638780）。
 * <p>
 * 这里只对超出安全整数范围的值输出字符串，范围内的小数字（如分页 total=308、
 * 库存、数量等）仍保持数字类型，避免影响前端的分页计算与数值比较。
 */
public class LongSafeSerializer extends JsonSerializer<Long> {

    public static final long MAX_SAFE_INTEGER = 9007199254740991L;

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (value > MAX_SAFE_INTEGER || value < -MAX_SAFE_INTEGER) {
            gen.writeString(String.valueOf(value));
        } else {
            gen.writeNumber(value);
        }
    }
}
