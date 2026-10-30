package com.linjian.seckill.domain.enums;

import lombok.Getter;

/**
 * 活动状态。落库为 tinyint，避免 MySQL 枚举改值时的 DDL 成本。
 */
@Getter
public enum ActivityStatus {

    /** 已创建，尚未开始，预热任务会扫描该状态 */
    PENDING(0, "待开始"),

    /** 已开始，Redis 中库存应已存在 */
    RUNNING(1, "进行中"),

    /** 已结束，库存键由清理任务回收 */
    FINISHED(2, "已结束"),

    /** 人工取消，不可再抢 */
    CANCELLED(3, "已取消");

    private final Integer code;
    private final String desc;

    ActivityStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ActivityStatus of(Integer code) {
        for (ActivityStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
