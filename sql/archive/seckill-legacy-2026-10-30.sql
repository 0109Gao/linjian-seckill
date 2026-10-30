-- MySQL dump 10.13  Distrib 26.10.0, for Win64 (x86_64)
--
-- Host: localhost    Database: seckill
-- ------------------------------------------------------
-- Server version	26.10.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '01dd8cdd-cf86-11f1-9bf5-890166fcbec4:1-194';

--
-- Table structure for table `seckill_goods`
--

DROP TABLE IF EXISTS `seckill_goods`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seckill_goods` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鍟嗗搧ID',
  `goods_name` varchar(128) NOT NULL COMMENT '鍟嗗搧鍚嶇О',
  `stock` int NOT NULL COMMENT '鎬诲簱瀛',
  `total_stock` int NOT NULL DEFAULT '0' COMMENT '初始总库存（扣减不影响）',
  `seckill_price` decimal(10,2) NOT NULL COMMENT '绉掓潃浠',
  `start_time` datetime NOT NULL COMMENT '绉掓潃寮??鏃堕棿',
  `end_time` datetime NOT NULL COMMENT '绉掓潃缁撴潫鏃堕棿',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绉掓潃鍟嗗搧琛';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `seckill_goods`
--

LOCK TABLES `seckill_goods` WRITE;
/*!40000 ALTER TABLE `seckill_goods` DISABLE KEYS */;
INSERT INTO `seckill_goods` VALUES (5,'iPhone 16 Pro Max 256G',100,100,5999.00,'2026-01-01 00:00:00','2026-12-31 23:59:59','2026-10-25 12:51:46'),(6,'华为 Mate 70 Pro',50,50,4999.00,'2026-01-01 00:00:00','2026-12-31 23:59:59','2026-10-25 12:51:46'),(7,'小米 15 Ultra',30,30,3999.00,'2026-01-01 00:00:00','2026-12-31 23:59:59','2026-10-25 12:51:46'),(8,'飞天茅台 53度',10,10,1499.00,'2026-01-01 00:00:00','2026-12-31 23:59:59','2026-10-25 12:51:46');
/*!40000 ALTER TABLE `seckill_goods` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `seckill_order`
--

DROP TABLE IF EXISTS `seckill_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seckill_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '璁㈠崟ID',
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID',
  `goods_id` bigint NOT NULL COMMENT '鍟嗗搧ID',
  `order_no` varchar(64) NOT NULL COMMENT '璁㈠崟鍙',
  `status` varchar(20) NOT NULL DEFAULT 'CREATED' COMMENT '璁㈠崟鐘舵? CREATED/PAID/CANCELLED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_goods` (`user_id`,`goods_id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绉掓潃璁㈠崟琛';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `seckill_order`
--

LOCK TABLES `seckill_order` WRITE;
/*!40000 ALTER TABLE `seckill_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `seckill_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鐢ㄦ埛ID',
  `username` varchar(64) NOT NULL COMMENT '鐢ㄦ埛鍚',
  `password` varchar(128) NOT NULL COMMENT '瀵嗙爜锛圔Crypt锛',
  `phone` varchar(20) DEFAULT NULL COMMENT '鎵嬫満鍙',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢ㄦ埛琛';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'seckill_user_1','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa','13800000001','2026-10-25 12:51:28'),(2,'seckill_user_2','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa','13800000002','2026-10-25 12:51:28'),(3,'seckill_user_3','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa','13800000003','2026-10-25 12:51:28');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-30 12:40:11
