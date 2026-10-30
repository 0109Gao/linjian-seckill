-- MySQL 官方 Docker 镜像的 initdb 阶段，mysql 客户端默认字符集是 latin1
-- （取决于容器 locale，通常为 C/POSIX）。若不显式声明，本文件里的 UTF-8 中文
-- 会被当作 latin1 字节读入，写进 utf8mb4 列时发生双重编码（mojibake）。
-- 因此必须在任何 DDL 之前 SET NAMES。这是 docker-library/mysql#131 的已知问题。
SET NAMES utf8mb4;

-- ============================================================
-- 林间秒杀系统 · 数据库初始化脚本
-- 适用：MySQL 8.x
-- 执行：mysql -u root -p < sql/schema.sql
-- 说明：W1 仅建用户表；商品/活动/订单相关表在 W2、W3 追加，
--       每次追加都要在本文件末尾补充，保持单一初始化入口。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `seckill`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `seckill`;

-- ------------------------------------------------------------
-- 用户表：C 端买家与后台管理员共用，role 区分
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sk_user`;
CREATE TABLE `sk_user`
(
    `id`          BIGINT       NOT NULL COMMENT '雪花 ID',
    `username`    VARCHAR(32)  NOT NULL COMMENT '登录名',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    `nickname`    VARCHAR(32)  DEFAULT NULL COMMENT '昵称',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `role`        VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常，0 禁用',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删，1 已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    -- 唯一索引必须带 deleted 以外的语义：用户名逻辑删除后仍不能重复注册，
    -- 因此这里不加 deleted 列，若将来需要允许重复注册再改为 (username, deleted)
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 初始管理员由应用启动时自动创建（见 AdminInitializer），
-- 此处不写死密码哈希，避免脚本中出现默认口令。

-- ------------------------------------------------------------
-- W2：商品 / 活动 / 活动商品关联
--
-- 建模选择「一活动多商品」（活动 = 专场，一个专场挂多个 SKU），而不是
-- 「一活动一商品」。代价是库存要按 (activity_id, goods_id) 二维定位，
-- Redis 键因此是 sk:stock:{activityId}:{goodsId}；收益是同一个专场可以
-- 同时卖多个商品，且库存粒度天然独立，不必在活动行上做数组拆分。
-- ------------------------------------------------------------

DROP TABLE IF EXISTS `sk_goods`;
CREATE TABLE `sk_goods`
(
    `id`           BIGINT         NOT NULL COMMENT '雪花 ID',
    `name`         VARCHAR(64)    NOT NULL COMMENT '商品名',
    `subtitle`     VARCHAR(128)   DEFAULT NULL COMMENT '副标题/卖点',
    `image_url`    VARCHAR(255)   DEFAULT NULL COMMENT '主图地址',
    `origin_price` DECIMAL(10, 2) NOT NULL COMMENT '原价，用于展示划线价',
    `status`       TINYINT        NOT NULL DEFAULT 1 COMMENT '1 上架，0 下架',
    `deleted`      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品表';

DROP TABLE IF EXISTS `sk_activity`;
CREATE TABLE `sk_activity`
(
    `id`          BIGINT       NOT NULL COMMENT '雪花 ID',
    `title`       VARCHAR(64)  NOT NULL COMMENT '活动标题',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '活动说明',
    `start_time`  DATETIME     NOT NULL COMMENT '开始时间',
    `end_time`    DATETIME     NOT NULL COMMENT '结束时间',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待开始，1 进行中，2 已结束，3 已取消',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    -- 预热的扫描条件是 status + start_time，两列顺序不能反：
    -- 反了会退化成全表扫描，活动多了以后预热任务会拖慢整个库
    KEY `idx_status_start` (`status`, `start_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='秒杀活动（专场）表';

DROP TABLE IF EXISTS `sk_activity_goods`;
CREATE TABLE `sk_activity_goods`
(
    `id`             BIGINT         NOT NULL COMMENT '雪花 ID',
    `activity_id`    BIGINT         NOT NULL COMMENT '活动 ID',
    `goods_id`       BIGINT         NOT NULL COMMENT '商品 ID',
    `seckill_price`  DECIMAL(10, 2) NOT NULL COMMENT '秒杀价',
    `stock`          INT            NOT NULL COMMENT '总库存（DB 侧扣减，Redis 侧为剩余库存）',
    `limit_per_user` INT            NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    -- 乐观锁版本号：DB 侧扣减必须带版本条件，
    -- Redis 只是第一道闸门，最终一致性要靠这里兜住
    `version`        INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    -- 同一活动下同一商品只能出现一次，这条唯一索引同时是
    -- 后台重复提交的兜底
    UNIQUE KEY `uk_activity_goods` (`activity_id`, `goods_id`),
    KEY `idx_goods` (`goods_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='活动商品关联表（库存主体）';

-- ------------------------------------------------------------
-- W3：订单 / 幂等表 / 本地消息表
--
-- 扣减时机采用「预扣即扣」：Redis 预扣成功后异步落单，DB 库存与订单
-- 在同一条消息的消费事务里递减。任何时刻两者差额都能用订单表解释，
-- 对账逻辑因此可以做到可证伪。
-- ------------------------------------------------------------

DROP TABLE IF EXISTS `sk_seckill_order`;
CREATE TABLE `sk_seckill_order`
(
    `id`                BIGINT         NOT NULL COMMENT '雪花 ID',
    `order_no`          VARCHAR(32)    NOT NULL COMMENT '业务订单号（展示给用户）',
    `request_id`        BIGINT         NOT NULL COMMENT '秒杀请求流水号，前端用它轮询结果',
    `user_id`           BIGINT         NOT NULL COMMENT '买家 ID',
    `activity_id`       BIGINT         NOT NULL COMMENT '活动 ID',
    `activity_goods_id` BIGINT         NOT NULL COMMENT '活动商品关联行 ID',
    `goods_id`          BIGINT         NOT NULL COMMENT '商品 ID',
    `goods_name`        VARCHAR(64)    NOT NULL COMMENT '下单时的商品名快照',
    `price`             DECIMAL(10, 2) NOT NULL COMMENT '成交价快照（不随商品改价变动）',
    `quantity`          INT            NOT NULL DEFAULT 1 COMMENT '购买数量',
    `status`            TINYINT        NOT NULL DEFAULT 0 COMMENT '0 待支付，1 已支付，2 已取消，3 已回补',
    `expire_time`       DATETIME       NOT NULL COMMENT '支付截止时间',
    `pay_time`          DATETIME       DEFAULT NULL COMMENT '支付时间',
    `cancel_time`       DATETIME       DEFAULT NULL COMMENT '取消时间',
    `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    -- requestId 全局唯一：消费者重复消费时靠它插入冲突，从而天然幂等
    UNIQUE KEY `uk_request` (`request_id`),
    -- 一人一活动一商品限购 1 件。这条索引是「超卖」的最后一道防线：
    -- 即使 Redis 用户防重被绕过，同一用户也无法插入第二行。
    -- 取消后不可再次抢购是有意为之，否则「抢到-取消-再抢」可以无限刷库存。
    UNIQUE KEY `uk_user_activity_goods` (`user_id`, `activity_id`, `goods_id`),
    -- 关单扫描走 (status, expire_time)，顺序不能反
    KEY `idx_timeout` (`status`, `expire_time`),
    KEY `idx_activity_status` (`activity_id`, `status`),
    KEY `idx_user` (`user_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='秒杀订单表';

DROP TABLE IF EXISTS `sk_idempotent_record`;
CREATE TABLE `sk_idempotent_record`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键（仅用于统计与定期清理）',
    `message_id`  VARCHAR(64) NOT NULL COMMENT '消息唯一标识，与发送侧一致',
    `biz_type`    VARCHAR(32) NOT NULL COMMENT '业务类型，如 SECKILL_ORDER',
    `biz_id`      VARCHAR(64) DEFAULT NULL COMMENT '业务 ID（订单号或 requestId）',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message` (`message_id`),
    KEY `idx_create` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='消费幂等表';

DROP TABLE IF EXISTS `sk_local_message`;
CREATE TABLE `sk_local_message`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT,
    `message_id`      VARCHAR(64) NOT NULL COMMENT '消息唯一标识（消费侧据此去重）',
    `exchange_name`   VARCHAR(64) NOT NULL COMMENT '目标交换机',
    `routing_key`     VARCHAR(64) NOT NULL COMMENT '路由键',
    `payload`         TEXT        NOT NULL COMMENT '消息体 JSON',
    `status`          TINYINT     NOT NULL DEFAULT 0 COMMENT '0 待投递，1 已投递，2 投递失败待重试',
    `retry_count`     INT         NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `next_retry_time` DATETIME    DEFAULT NULL COMMENT '下次重试时间（退避）',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message` (`message_id`),
    KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='本地消息表（保证 DB 提交与 MQ 投递最终一致）';
