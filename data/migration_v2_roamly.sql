-- ============================================================
-- Roamly 功能扩展迁移脚本 v2
-- 灵感目的地 / 旅程记录 / 精细化用户偏好
-- 基于现有 travel_agent 数据库，新增表 + 新列
-- 兼容 MySQL 5.7+ / 8.0+（不使用 ADD COLUMN IF NOT EXISTS）
-- ============================================================

USE travel_agent;

-- -----------------------------------------------------------
-- 辅助存储过程：安全添加列（列已存在时跳过）
-- -----------------------------------------------------------
DROP PROCEDURE IF EXISTS add_col;
DELIMITER ;;
CREATE PROCEDURE add_col(
    IN p_table VARCHAR(128),
    IN p_column VARCHAR(128),
    IN p_definition TEXT
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'travel_agent'
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column;

    IF col_count = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_column, ' ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;;
DELIMITER ;

-- -----------------------------------------------------------
-- 1. 用户表扩展
-- -----------------------------------------------------------
CALL add_col('user_profile', 'phone',        'VARCHAR(20) NOT NULL DEFAULT \'\' AFTER email');
CALL add_col('user_profile', 'avatar_url',   'VARCHAR(512) NOT NULL DEFAULT \'\' AFTER avatar');

-- -----------------------------------------------------------
-- 2. 用户偏好表扩展（精细化字段）
-- -----------------------------------------------------------
CALL add_col('user_travel_preference', 'default_departure_city',  'VARCHAR(64) NOT NULL DEFAULT \'\' AFTER favorite_destinations');
CALL add_col('user_travel_preference', 'default_days',            'INT NOT NULL DEFAULT 3 AFTER default_departure_city');
CALL add_col('user_travel_preference', 'default_budget',          'INT NOT NULL DEFAULT 3000 AFTER default_days');
CALL add_col('user_travel_preference', 'default_travelers',       'INT NOT NULL DEFAULT 2 AFTER default_budget');
CALL add_col('user_travel_preference', 'preferred_season_detail', 'VARCHAR(32) NOT NULL DEFAULT \'\' AFTER preferred_season');
CALL add_col('user_travel_preference', 'preferred_month',         'VARCHAR(64) NOT NULL DEFAULT \'\' AFTER preferred_season_detail');
CALL add_col('user_travel_preference', 'preferred_trip_type',     'VARCHAR(32) NOT NULL DEFAULT \'\' AFTER preferred_month');

CALL add_col('user_travel_preference', 'hotel_star_min',           'TINYINT NOT NULL DEFAULT 3 AFTER accommodation_requirements');
CALL add_col('user_travel_preference', 'hotel_budget_per_night_min','INT NOT NULL DEFAULT 0 AFTER hotel_star_min');
CALL add_col('user_travel_preference', 'hotel_budget_per_night_max','INT NOT NULL DEFAULT 0 AFTER hotel_budget_per_night_min');
CALL add_col('user_travel_preference', 'preferred_hotel_type',     'VARCHAR(64) NOT NULL DEFAULT \'\' AFTER hotel_budget_per_night_max');

CALL add_col('user_travel_preference', 'seat_preference',          'VARCHAR(32) NOT NULL DEFAULT \'\' AFTER transportation_preference');
CALL add_col('user_travel_preference', 'max_transit_duration',     'INT NOT NULL DEFAULT 0 AFTER seat_preference');

CALL add_col('user_travel_preference', 'cuisine_preferences',      'JSON AFTER preferred_cuisines');
CALL add_col('user_travel_preference', 'meal_budget_per_person',   'INT NOT NULL DEFAULT 0 AFTER cuisine_preferences');
CALL add_col('user_travel_preference', 'spicy_level',              'TINYINT NOT NULL DEFAULT 0 AFTER meal_budget_per_person');

CALL add_col('user_travel_preference', 'attraction_types',          'JSON AFTER interests');
CALL add_col('user_travel_preference', 'max_attractions_per_day',   'INT NOT NULL DEFAULT 4 AFTER attraction_types');
CALL add_col('user_travel_preference', 'prefer_free_attractions',   'TINYINT NOT NULL DEFAULT 0 AFTER max_attractions_per_day');

CALL add_col('user_travel_preference', 'shopping_preference',      'VARCHAR(32) NOT NULL DEFAULT \'\' AFTER mobility_requirements');
CALL add_col('user_travel_preference', 'shopping_budget',          'INT NOT NULL DEFAULT 0 AFTER shopping_preference');

CALL add_col('user_travel_preference', 'has_elderly',              'TINYINT NOT NULL DEFAULT 0 AFTER children_ages');
CALL add_col('user_travel_preference', 'has_disability',           'TINYINT NOT NULL DEFAULT 0 AFTER has_elderly');

CALL add_col('user_travel_preference', 'notify_before_trip_days',  'INT NOT NULL DEFAULT 3 AFTER special_requests');
CALL add_col('user_travel_preference', 'notify_weather_alert',     'TINYINT NOT NULL DEFAULT 1 AFTER notify_before_trip_days');
CALL add_col('user_travel_preference', 'notify_price_change',      'TINYINT NOT NULL DEFAULT 1 AFTER notify_weather_alert');
CALL add_col('user_travel_preference', 'preferred_language',       'VARCHAR(16) NOT NULL DEFAULT \'zh-CN\' AFTER notify_price_change');

-- -----------------------------------------------------------
-- 3. 灵感目的地表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS inspiration (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id         VARCHAR(64)     NOT NULL                        COMMENT '用户唯一标识',
    name            VARCHAR(128)    NOT NULL DEFAULT ''             COMMENT '目的地名称',
    image_url       VARCHAR(1024)   NOT NULL DEFAULT ''             COMMENT '配图 URL',
    quote           VARCHAR(512)    NOT NULL DEFAULT ''             COMMENT '语录/期待',
    description     TEXT                                            COMMENT '详细描述',
    tags            JSON                                            COMMENT '标签 JSON数组',
    priority        TINYINT         NOT NULL DEFAULT 0              COMMENT '优先级 0普通 1想去 2非常想去',
    estimated_budget INT            NOT NULL DEFAULT 0              COMMENT '预估预算(元)',
    best_season     VARCHAR(32)     NOT NULL DEFAULT ''             COMMENT '最佳季节',
    status          TINYINT         NOT NULL DEFAULT 1              COMMENT '状态 1-活跃 0-已达成/归档',
    achieved_at     DATETIME        NULL                            COMMENT '达成时间',
    sort_order      INT             NOT NULL DEFAULT 0              COMMENT '排序权重',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灵感目的地表';

-- -----------------------------------------------------------
-- 4. 旅程记录表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS journey (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id         VARCHAR(64)     NOT NULL                        COMMENT '用户唯一标识',
    destination     VARCHAR(128)    NOT NULL DEFAULT ''             COMMENT '主要目的地',
    departure_city  VARCHAR(64)     NOT NULL DEFAULT ''             COMMENT '出发城市',
    start_date      DATE            NOT NULL                        COMMENT '出发日期',
    end_date        DATE            NOT NULL                        COMMENT '结束日期',
    total_days      INT             NOT NULL DEFAULT 0              COMMENT '总天数',
    summary         TEXT            NOT NULL                        COMMENT '旅程总结(几百字)',
    total_cost      INT             NOT NULL DEFAULT 0              COMMENT '总花费(元)',
    rating          TINYINT         NOT NULL DEFAULT 0              COMMENT '评分 1-5',
    travel_type     VARCHAR(32)     NOT NULL DEFAULT ''             COMMENT '旅行类型 自由行/跟团/自驾等',
    companions      VARCHAR(64)     NOT NULL DEFAULT ''             COMMENT '同行人',
    weather_info    VARCHAR(128)    NOT NULL DEFAULT ''             COMMENT '出行天气概况',
    highlight       TEXT                                            COMMENT '最难忘的瞬间',
    tips            TEXT                                            COMMENT '给后来者的建议',
    status          TINYINT         NOT NULL DEFAULT 1              COMMENT '状态 1-已记录 2-草稿',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_start_date (start_date),
    INDEX idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='旅程记录表';

-- -----------------------------------------------------------
-- 5. 旅程途经地点表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS journey_point (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    journey_id      BIGINT          NOT NULL                        COMMENT '旅程 ID',
    name            VARCHAR(128)    NOT NULL DEFAULT ''             COMMENT '地点名称',
    latitude        DECIMAL(10,7)   NOT NULL                        COMMENT '纬度',
    longitude       DECIMAL(10,7)   NOT NULL                        COMMENT '经度',
    visit_date      DATE            NULL                            COMMENT '到访日期',
    description     VARCHAR(512)    NOT NULL DEFAULT ''             COMMENT '地点描述',
    sort_order      INT             NOT NULL DEFAULT 0              COMMENT '路线顺序',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_journey_id (journey_id),
    CONSTRAINT fk_pt_journey FOREIGN KEY (journey_id) REFERENCES journey(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='旅程途经地点表';

-- -----------------------------------------------------------
-- 6. 旅程照片表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS journey_image (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    journey_id      BIGINT          NOT NULL                        COMMENT '旅程 ID',
    image_url       VARCHAR(1024)   NOT NULL DEFAULT ''             COMMENT '图片 URL',
    caption         VARCHAR(256)    NOT NULL DEFAULT ''             COMMENT '图片说明',
    sort_order      INT             NOT NULL DEFAULT 0              COMMENT '排序',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_journey_id (journey_id),
    CONSTRAINT fk_img_journey FOREIGN KEY (journey_id) REFERENCES journey(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='旅程照片表';

-- -----------------------------------------------------------
-- 7. 历史规划记录表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS travel_plan_history (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id         VARCHAR(64)     NOT NULL                        COMMENT '用户唯一标识',
    plan_id         VARCHAR(64)     NOT NULL UNIQUE                 COMMENT '规划唯一标识',
    destination     VARCHAR(128)    NOT NULL DEFAULT ''             COMMENT '目的地',
    days            INT             NOT NULL DEFAULT 0              COMMENT '天数',
    total_budget    INT             NOT NULL DEFAULT 0              COMMENT '总预算',
    travelers       INT             NOT NULL DEFAULT 1              COMMENT '人数',
    travel_style    VARCHAR(32)     NOT NULL DEFAULT ''             COMMENT '旅行风格',
    interests       JSON                                            COMMENT '兴趣标签',
    plan_content    JSON                                            COMMENT '完整行程 JSON',
    is_streaming    TINYINT         NOT NULL DEFAULT 0              COMMENT '是否流式生成',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_plan_id (plan_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='历史规划记录表';

-- 清理辅助存储过程
DROP PROCEDURE IF EXISTS add_col;

-- -----------------------------------------------------------
-- 示例数据
-- -----------------------------------------------------------
INSERT INTO inspiration (user_id, name, image_url, quote, priority, best_season) VALUES
('user_001', '冰岛雷克雅未克', '', '去看一次极光，在世界的尽头', 2, '冬'),
('user_001', '云南大理', '', '苍山洱海，风花雪月', 1, '春'),
('user_001', '日本京都', '', '在千年古都的巷子里迷路', 1, '秋');

INSERT INTO journey (user_id, destination, departure_city, start_date, end_date, total_days,
    summary, total_cost, rating, travel_type, highlight)
VALUES
('user_001', '杭州', '北京', '2026-04-01', '2026-04-04', 4,
    '清明时节去了杭州，四月的西湖烟雨朦胧，别有一番诗意。第一天漫步苏堤，看断桥残雪虽已无雪，但白堤两旁的桃花开得正盛。第二天去了灵隐寺，在飞来峰下感受千年古刹的宁静。第三天骑行龙井村，满山茶园，空气里都是清新的茶香。最后一天在河坊街逛吃，买了些丝绸和糕点带给家人。杭州的美，是那种需要慢慢品的温润。',
    4200, 5, '自由行', '在龙井村的茶园里骑行，满眼翠绿，风吹过时茶香扑鼻');

INSERT INTO journey_point (journey_id, name, latitude, longitude, sort_order)
SELECT id, '西湖·苏堤', 30.2500000, 120.1350000, 1 FROM journey WHERE destination = '杭州';
INSERT INTO journey_point (journey_id, name, latitude, longitude, sort_order)
SELECT id, '灵隐寺', 30.2470000, 120.0980000, 2 FROM journey WHERE destination = '杭州';
INSERT INTO journey_point (journey_id, name, latitude, longitude, sort_order)
SELECT id, '龙井村', 30.2200000, 120.1200000, 3 FROM journey WHERE destination = '杭州';
INSERT INTO journey_point (journey_id, name, latitude, longitude, sort_order)
SELECT id, '河坊街', 30.2420000, 120.1700000, 4 FROM journey WHERE destination = '杭州';