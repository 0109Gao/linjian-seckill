package com.linjian.seckill.common;

/**
 * 雪花算法 ID 生成器。
 * <p>
 * 结构：1 位符号位（恒 0）+ 41 位时间戳（毫秒，可用约 69 年）+ 5 位数据中心 + 5 位机器号 + 12 位序列号。
 * <p>
 * 为什么订单表不用数据库自增：秒杀场景下订单号会暴露真实销量，
 * 且分布式扩展时自增 ID 无法保证全局唯一。雪花 ID 兼顾趋势递增与无中心化。
 */
public class SnowflakeIdGenerator {

    /** 起始纪元：2026-01-01 00:00:00 UTC */
    private static final long EPOCH = 1767225600000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    private static volatile SnowflakeIdGenerator instance;

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId 必须在 0~" + MAX_WORKER_ID + " 之间");
        }
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("datacenterId 必须在 0~" + MAX_DATACENTER_ID + " 之间");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public static SnowflakeIdGenerator getInstance() {
        if (instance == null) {
            synchronized (SnowflakeIdGenerator.class) {
                if (instance == null) {
                    instance = new SnowflakeIdGenerator(0L, 0L);
                }
            }
        }
        return instance;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            // 时钟回拨：回拨幅度小则等待，幅度大则直接抛错，绝不生成重复 ID
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                timestamp = waitUntil(lastTimestamp);
            } else {
                throw new IllegalStateException("系统时钟回拨 " + offset + " 毫秒，拒绝生成 ID");
            }
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 同一毫秒内序列号耗尽，等待下一毫秒
                timestamp = waitUntil(lastTimestamp + 1);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitUntil(long targetTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp < targetTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
