-- 同 schema.sql：initdb 阶段客户端默认 latin1，必须 SET NAMES 防止中文双重编码。
SET NAMES utf8mb4;

-- ============================================================
-- 开发演示数据（可选，勿在生产执行）
-- 执行：mysql -u root -p < sql/seed-dev.sql
--
-- 说明：所有示例数据都用固定 ID（活动 1001，商品 2001-2003），
-- 重复执行会先按 ID 删除再插入，因此是幂等的。
-- ============================================================

USE `seckill`;

-- ------------------------------------------------------------
-- 演示账号（密码均为 123456）
--
-- 这里直接写 BCrypt 哈希而不是明文：sk_user.password 列存的是
-- BCrypt($2a$10$...) 密文，写明文会导致登录必然失败，且看不出原因
-- （报错是「用户名或密码错误」，跟账号不存在长得一样）。
-- 哈希由应用自身的 BCryptPasswordEncoder 生成，保证与线上校验口径一致。
-- ------------------------------------------------------------
DELETE FROM `sk_user` WHERE `username` IN ('admin', 'buyer01');

INSERT INTO `sk_user` (`id`, `username`, `password`, `nickname`, `phone`, `role`, `status`)
VALUES (1000, 'admin',
        '$2a$10$AS8XzUlWJWB1HMybMHqWouaheT1quocdL8UfzosHnL8KYjK2R/f5S', '系统管理员', NULL, 'ADMIN', 1),
       (1001, 'buyer01',
        '$2a$10$AS8XzUlWJWB1HMybMHqWouaheT1quocdL8UfzosHnL8KYjK2R/f5S', '测试买家', NULL, 'USER', 1);

DELETE FROM `sk_activity_goods` WHERE `activity_id` = 1001;
DELETE FROM `sk_activity` WHERE `id` = 1001;
DELETE FROM `sk_goods` WHERE `id` IN (2001, 2002, 2003);

INSERT INTO `sk_goods` (`id`, `name`, `subtitle`, `image_url`, `origin_price`, `status`)
VALUES (2001, '林间手冲咖啡豆 250g', '中浅烘焙 · 柑橘尾韵', NULL, 128.00, 1),
       (2002, '冷萃咖啡液 6 瓶装', '零糖零添加 · 冷藏直达', NULL, 96.00, 1),
       (2003, '手摇磨豆机', '六星刀盘 · 便携装', NULL, 239.00, 1);

-- 活动在 10 分钟后开始，因此定时任务（提前 5 分钟）会在启动后的一轮扫描里
-- 自动完成预热，可以用来验证预热链路
INSERT INTO `sk_activity` (`id`, `title`, `description`, `start_time`, `end_time`, `status`)
VALUES (1001, '周五晚八点 · 咖啡专场', '限量秒杀，每人限购 1 件',
        DATE_ADD(NOW(), INTERVAL 10 MINUTE),
        DATE_ADD(NOW(), INTERVAL 130 MINUTE), 0);

INSERT INTO `sk_activity_goods` (`id`, `activity_id`, `goods_id`, `seckill_price`, `stock`, `limit_per_user`)
VALUES (3001, 1001, 2001, 39.90, 100, 1),
       (3002, 1001, 2002, 29.90, 50, 1),
       (3003, 1001, 2003, 99.00, 20, 1);
