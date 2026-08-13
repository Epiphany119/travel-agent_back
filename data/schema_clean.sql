-- ============================================================
-- Roamly 旅行助手 - 数据库建表脚本（纯净版，无初始数据）
-- 数据库: travel_agent
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `travel_agent` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `travel_agent`;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `nickname` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `frequent_destination` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '常去目的地',
  `default_preferences` text COLLATE utf8mb4_unicode_ci COMMENT '默认偏好JSON',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 2. 用户信息表
-- ----------------------------
DROP TABLE IF EXISTS `user_profile`;
CREATE TABLE `user_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户唯一标识',
  `username` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `nickname` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `avatar_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `email` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `bio` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '个人简介',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-禁用, 1-正常',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- ----------------------------
-- 3. 用户旅行偏好表
-- ----------------------------
DROP TABLE IF EXISTS `user_travel_preference`;
CREATE TABLE `user_travel_preference` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户唯一标识',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '用户名',
  `email` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '邮箱',
  `phone` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '手机号',
  `preference_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'default' COMMENT '偏好类型: default-默认偏好, custom-自定义偏好',
  `preference_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '偏好名称，如"商务出行"、"亲子游"',
  `favorite_destinations` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '常去目的地，逗号分隔',
  `default_departure_city` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `default_days` int NOT NULL DEFAULT '3',
  `default_budget` int NOT NULL DEFAULT '3000',
  `default_travelers` int NOT NULL DEFAULT '2',
  `preferred_season` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '偏好出行季节',
  `preferred_season_detail` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `preferred_month` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `preferred_trip_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `budget_level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'standard' COMMENT '预算等级: economy-经济型, standard-标准型, luxury-豪华型',
  `daily_budget_min` int DEFAULT NULL COMMENT '日预算下限',
  `daily_budget_max` int DEFAULT NULL COMMENT '日预算上限',
  `travel_style` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '旅行风格: 轻松漫游, 深度人文, 美食优先, 亲子友好, 探险挑战',
  `interests` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '兴趣标签，逗号分隔: 美食, 人文, 自然, 摄影, 购物, 夜生活',
  `attraction_types` json DEFAULT NULL,
  `max_attractions_per_day` int NOT NULL DEFAULT '4',
  `prefer_free_attractions` tinyint NOT NULL DEFAULT '0',
  `dietary_requirements` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '饮食要求: 无辣, 素食, 清真, 海鲜过敏等',
  `preferred_cuisines` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '偏好菜系，逗号分隔',
  `cuisine_preferences` json DEFAULT NULL,
  `meal_budget_per_person` int NOT NULL DEFAULT '0',
  `spicy_level` tinyint NOT NULL DEFAULT '0',
  `accommodation_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '住宿类型: 经济酒店, 民宿, 豪华酒店, 青旅',
  `accommodation_requirements` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '住宿要求: 近地铁, 含早, 有泳池等',
  `hotel_star_min` tinyint NOT NULL DEFAULT '3',
  `hotel_budget_per_night_min` int NOT NULL DEFAULT '0',
  `hotel_budget_per_night_max` int NOT NULL DEFAULT '0',
  `preferred_hotel_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `transportation_preference` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '交通偏好: 高铁, 飞机, 自驾, 大巴',
  `seat_preference` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `max_transit_duration` int NOT NULL DEFAULT '0',
  `travel_companion` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '出行人群: solo-独自, couple-情侣, family-家庭, friends-朋友, business-商务',
  `has_children` tinyint DEFAULT '0' COMMENT '是否有儿童',
  `children_ages` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '儿童年龄段',
  `has_elderly` tinyint NOT NULL DEFAULT '0',
  `has_disability` tinyint NOT NULL DEFAULT '0',
  `activity_level` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'moderate' COMMENT '活动强度: relaxed-休闲, moderate-适中, active-活跃',
  `pace_preference` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节奏偏好: 松散, 适中, 紧凑',
  `mobility_requirements` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '行动需求: 无障碍, 轮椅友好等',
  `shopping_preference` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `shopping_budget` int NOT NULL DEFAULT '0',
  `special_requests` text COLLATE utf8mb4_unicode_ci COMMENT '特殊要求，如老人随行, 宠物同行等',
  `notify_before_trip_days` int NOT NULL DEFAULT '3',
  `notify_weather_alert` tinyint NOT NULL DEFAULT '1',
  `notify_price_change` tinyint NOT NULL DEFAULT '1',
  `preferred_language` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'zh-CN',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_preference_type` (`preference_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户旅行偏好表';

-- ----------------------------
-- 4. 用户目的地偏好表
-- ----------------------------
DROP TABLE IF EXISTS `user_destination_preference`;
CREATE TABLE `user_destination_preference` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户唯一标识',
  `destination` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目的地城市',
  `province` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '省份',
  `country` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '中国' COMMENT '国家',
  `visit_count` int DEFAULT '0' COMMENT '去过次数',
  `rating` int DEFAULT NULL COMMENT '评分 1-5',
  `favorite_season` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最佳旅游季节',
  `least_favorite_season` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '不推荐季节',
  `recommended_attractions` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '推荐景点，逗号分隔',
  `recommended_restaurants` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '推荐餐厅，逗号分隔',
  `tips` text COLLATE utf8mb4_unicode_ci COMMENT '个人心得',
  `last_visit_cost` int DEFAULT NULL COMMENT '上次旅行费用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_destination` (`user_id`,`destination`),
  KEY `idx_user_destination` (`user_id`,`destination`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户目的地偏好表';

-- ----------------------------
-- 5. 灵感目的地表
-- ----------------------------
DROP TABLE IF EXISTS `inspiration`;
CREATE TABLE `inspiration` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户唯一标识',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '目的地名称',
  `image_url` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '配图 URL',
  `quote` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '语录/期待',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '详细描述',
  `tags` json DEFAULT NULL COMMENT '标签 JSON数组',
  `priority` tinyint NOT NULL DEFAULT '0' COMMENT '优先级 0普通 1想去 2非常想去',
  `estimated_budget` int NOT NULL DEFAULT '0' COMMENT '预估预算(元)',
  `best_season` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '最佳季节',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1-活跃 0-已达成/归档',
  `achieved_at` datetime DEFAULT NULL COMMENT '达成时间',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灵感目的地表';

-- ----------------------------
-- 6. 旅程记录表
-- ----------------------------
DROP TABLE IF EXISTS `journey`;
CREATE TABLE `journey` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户唯一标识',
  `destination` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '主要目的地',
  `departure_city` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '出发城市',
  `start_date` date NOT NULL COMMENT '出发日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `total_days` int NOT NULL DEFAULT '0' COMMENT '总天数',
  `summary` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '旅程总结(几百字)',
  `total_cost` int NOT NULL DEFAULT '0' COMMENT '总花费(元)',
  `rating` tinyint NOT NULL DEFAULT '0' COMMENT '评分 1-5',
  `travel_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '旅行类型 自由行/跟团/自驾等',
  `companions` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '同行人',
  `weather_info` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '出行天气概况',
  `highlight` text COLLATE utf8mb4_unicode_ci COMMENT '最难忘的瞬间',
  `tips` text COLLATE utf8mb4_unicode_ci COMMENT '给后来者的建议',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1-已记录 2-草稿',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_start_date` (`start_date`),
  KEY `idx_rating` (`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='旅程记录表';

-- ----------------------------
-- 7. 旅程途经地点表
-- ----------------------------
DROP TABLE IF EXISTS `journey_point`;
CREATE TABLE `journey_point` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `journey_id` bigint NOT NULL COMMENT '旅程 ID',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '地点名称',
  `latitude` decimal(10,7) NOT NULL COMMENT '纬度',
  `longitude` decimal(10,7) NOT NULL COMMENT '经度',
  `visit_date` date DEFAULT NULL COMMENT '到访日期',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '地点描述',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '路线顺序',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_journey_id` (`journey_id`),
  CONSTRAINT `fk_pt_journey` FOREIGN KEY (`journey_id`) REFERENCES `journey` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='旅程途经地点表';

-- ----------------------------
-- 8. 旅程照片表
-- ----------------------------
DROP TABLE IF EXISTS `journey_image`;
CREATE TABLE `journey_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `journey_id` bigint NOT NULL COMMENT '旅程 ID',
  `image_url` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '图片 URL',
  `caption` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '图片说明',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_journey_id` (`journey_id`),
  CONSTRAINT `fk_img_journey` FOREIGN KEY (`journey_id`) REFERENCES `journey` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='旅程照片表';

-- ----------------------------
-- 9. 聊天会话表
-- ----------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话标题',
  `status` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'active' COMMENT '状态: active/completed/expired',
  `preference_json` text COLLATE utf8mb4_unicode_ci COMMENT '偏好JSON',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `last_active_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_last_active` (`last_active_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- ----------------------------
-- 10. 聊天消息表
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息ID',
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `role` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色: user/assistant/system/tool',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `tool_call_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工具调用ID',
  `tool_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工具名称',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_message_id` (`message_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- ----------------------------
-- 11. 行程表
-- ----------------------------
DROP TABLE IF EXISTS `itinerary`;
CREATE TABLE `itinerary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `itinerary_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '行程ID',
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `destination` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目的地',
  `start_date` date DEFAULT NULL COMMENT '出发日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `total_budget` decimal(12,2) DEFAULT NULL COMMENT '总预算',
  `days` int DEFAULT NULL COMMENT '行程天数',
  `day_plans_json` longtext COLLATE utf8mb4_unicode_ci COMMENT '每日行程JSON',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_itinerary_id` (`itinerary_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程表';

-- ----------------------------
-- 12. 每日行程表
-- ----------------------------
DROP TABLE IF EXISTS `day_plan`;
CREATE TABLE `day_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `itinerary_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '行程ID',
  `day_number` int NOT NULL COMMENT '第几天',
  `plan_date` date DEFAULT NULL COMMENT '日期',
  `theme` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主题',
  `day_budget` decimal(10,2) DEFAULT NULL COMMENT '当日预算',
  `notes` text COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_itinerary_id` (`itinerary_id`),
  KEY `idx_day_number` (`day_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日行程表';

-- ----------------------------
-- 13. 景点参观记录表
-- ----------------------------
DROP TABLE IF EXISTS `attraction_visit`;
CREATE TABLE `attraction_visit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `day_plan_id` bigint NOT NULL COMMENT '每日行程ID',
  `visit_order` int NOT NULL COMMENT '参观顺序',
  `attraction_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '景点名称',
  `attraction_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '景点类型',
  `address` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地址',
  `location` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '经纬度',
  `duration` int DEFAULT NULL COMMENT '游览时长(分钟)',
  `ticket_price` decimal(10,2) DEFAULT NULL COMMENT '门票价格',
  `opening_hours` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '营业时间',
  `tips` text COLLATE utf8mb4_unicode_ci COMMENT '游览提示',
  `rating` decimal(2,1) DEFAULT NULL COMMENT '评分',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_day_plan_id` (`day_plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点参观记录表';

-- ----------------------------
-- 14. 餐饮推荐表
-- ----------------------------
DROP TABLE IF EXISTS `meal_recommendation`;
CREATE TABLE `meal_recommendation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `day_plan_id` bigint NOT NULL COMMENT '每日行程ID',
  `meal_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '餐食类型: breakfast/lunch/dinner',
  `restaurant_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '餐厅名称',
  `cuisine_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '菜系',
  `address` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地址',
  `location` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '经纬度',
  `avg_price` decimal(10,2) DEFAULT NULL COMMENT '人均价格',
  `rating` decimal(2,1) DEFAULT NULL COMMENT '评分',
  `reason` text COLLATE utf8mb4_unicode_ci COMMENT '推荐理由',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_day_plan_id` (`day_plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='餐饮推荐表';

-- ----------------------------
-- 15. 历史规划记录表
-- ----------------------------
DROP TABLE IF EXISTS `travel_plan_history`;
CREATE TABLE `travel_plan_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户唯一标识',
  `plan_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规划唯一标识',
  `destination` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '目的地',
  `days` int NOT NULL DEFAULT '0' COMMENT '天数',
  `total_budget` int NOT NULL DEFAULT '0' COMMENT '总预算',
  `travelers` int NOT NULL DEFAULT '1' COMMENT '人数',
  `travel_style` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '旅行风格',
  `interests` json DEFAULT NULL COMMENT '兴趣标签',
  `plan_content` json DEFAULT NULL COMMENT '完整行程 JSON',
  `is_streaming` tinyint NOT NULL DEFAULT '0' COMMENT '是否流式生成',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `plan_id` (`plan_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='历史规划记录表';