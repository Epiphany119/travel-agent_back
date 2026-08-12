-- Travel Agent Database Initialization Script
-- This script is executed when MySQL container starts for the first time

CREATE DATABASE IF NOT EXISTS travel_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE travel_agent;

-- =====================================================
-- User Profile Tables (PR#4: User Profile & Preferences)
-- =====================================================

-- 用户基本信息表
CREATE TABLE IF NOT EXISTS user_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL UNIQUE COMMENT '用户唯一标识',
    username VARCHAR(100) NOT NULL COMMENT '用户名',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    email VARCHAR(200) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    bio VARCHAR(500) COMMENT '个人简介',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- 用户旅行偏好表
CREATE TABLE IF NOT EXISTS user_travel_preference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户唯一标识',
    preference_type VARCHAR(50) NOT NULL DEFAULT 'default' COMMENT '偏好类型: default-默认偏好, custom-自定义偏好',
    preference_name VARCHAR(100) COMMENT '偏好名称，如"商务出行"、"亲子游"',
    
    -- 目的地偏好
    favorite_destinations VARCHAR(500) COMMENT '常去目的地，逗号分隔',
    preferred_season VARCHAR(100) COMMENT '偏好出行季节',
    
    -- 预算偏好
    budget_level VARCHAR(20) DEFAULT 'standard' COMMENT '预算等级: economy-经济型, standard-标准型, luxury-豪华型',
    daily_budget_min INT COMMENT '日预算下限',
    daily_budget_max INT COMMENT '日预算上限',
    
    -- 旅行风格
    travel_style VARCHAR(100) COMMENT '旅行风格: 轻松漫游, 深度人文, 美食优先, 亲子友好, 探险挑战',
    interests VARCHAR(500) COMMENT '兴趣标签，逗号分隔: 美食, 人文, 自然, 摄影, 购物, 夜生活',
    
    -- 饮食偏好
    dietary_requirements VARCHAR(200) COMMENT '饮食要求: 无辣, 素食, 清真, 海鲜过敏等',
    preferred_cuisines VARCHAR(200) COMMENT '偏好菜系，逗号分隔',
    
    -- 住宿偏好
    accommodation_type VARCHAR(100) COMMENT '住宿类型: 经济酒店, 民宿, 豪华酒店, 青旅',
    accommodation_requirements VARCHAR(200) COMMENT '住宿要求: 近地铁, 含早, 有泳池等',
    
    -- 交通偏好
    transportation_preference VARCHAR(100) COMMENT '交通偏好: 高铁, 飞机, 自驾, 大巴',
    
    -- 同行偏好
    travel_companion VARCHAR(50) COMMENT '出行人群: solo-独自, couple-情侣, family-家庭, friends-朋友, business-商务',
    has_children TINYINT DEFAULT 0 COMMENT '是否有儿童',
    children_ages VARCHAR(100) COMMENT '儿童年龄段',
    
    -- 其他偏好
    activity_level VARCHAR(50) DEFAULT 'moderate' COMMENT '活动强度: relaxed-休闲, moderate-适中, active-活跃',
    pace_preference VARCHAR(50) COMMENT '节奏偏好: 松散, 适中, 紧凑',
    mobility_requirements VARCHAR(200) COMMENT '行动需求: 无障碍, 轮椅友好等',
    
    -- 特殊需求
    special_requests TEXT COMMENT '特殊要求，如老人随行, 宠物同行等',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_preference_type (preference_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户旅行偏好表';

-- 用户目的地偏好表（更细粒度的目的地信息）
CREATE TABLE IF NOT EXISTS user_destination_preference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户唯一标识',
    destination VARCHAR(100) NOT NULL COMMENT '目的地城市',
    province VARCHAR(50) COMMENT '省份',
    country VARCHAR(50) DEFAULT '中国' COMMENT '国家',
    
    -- 目的地评价
    visit_count INT DEFAULT 0 COMMENT '去过次数',
    rating INT COMMENT '评分 1-5',
    favorite_season VARCHAR(50) COMMENT '最佳旅游季节',
    least_favorite_season VARCHAR(50) COMMENT '不推荐季节',
    
    -- 细分偏好
    recommended_attractions VARCHAR(500) COMMENT '推荐景点，逗号分隔',
    recommended_restaurants VARCHAR(500) COMMENT '推荐餐厅，逗号分隔',
    tips TEXT COMMENT '个人心得',
    
    -- 费用记录
    last_visit_cost INT COMMENT '上次旅行费用',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_destination (user_id, destination),
    UNIQUE KEY uk_user_destination (user_id, destination)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户目的地偏好表';

-- =====================================================
-- Chat Session Tables
-- =====================================================

CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话唯一标识',
    user_id VARCHAR(64) COMMENT '用户唯一标识',
    title VARCHAR(255) COMMENT '会话标题',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态: active-进行中, closed-已关闭',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant/system/tool',
    content TEXT COMMENT '消息内容',
    tool_call_id VARCHAR(64) COMMENT '工具调用ID',
    tool_name VARCHAR(100) COMMENT '工具名称',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_message_id (message_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- =====================================================
-- Travel Planning Tables
-- =====================================================

CREATE TABLE IF NOT EXISTS itinerary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    itinerary_id VARCHAR(64) NOT NULL UNIQUE COMMENT '行程ID',
    session_id VARCHAR(64) COMMENT '关联会话ID',
    user_id VARCHAR(64) COMMENT '用户ID',
    destination VARCHAR(100) NOT NULL COMMENT '目的地',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    days INT COMMENT '天数',
    total_budget DECIMAL(10,2) COMMENT '总预算',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '状态: draft-草稿, published-已发布',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_itinerary_id (itinerary_id),
    INDEX idx_user_id (user_id),
    INDEX idx_destination (destination)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程表';

CREATE TABLE IF NOT EXISTS day_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    itinerary_id VARCHAR(64) NOT NULL COMMENT '行程ID',
    day_number INT NOT NULL COMMENT '第几天',
    plan_date DATE COMMENT '日期',
    theme VARCHAR(100) COMMENT '主题',
    day_budget DECIMAL(10,2) COMMENT '当日预算',
    notes TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_itinerary_id (itinerary_id),
    INDEX idx_day_number (day_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日行程表';

CREATE TABLE IF NOT EXISTS attraction_visit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    day_plan_id BIGINT NOT NULL COMMENT '每日行程ID',
    visit_order INT NOT NULL COMMENT '参观顺序',
    attraction_name VARCHAR(200) NOT NULL COMMENT '景点名称',
    attraction_type VARCHAR(100) COMMENT '景点类型',
    address VARCHAR(300) COMMENT '地址',
    location VARCHAR(100) COMMENT '经纬度',
    duration INT COMMENT '游览时长(分钟)',
    ticket_price DECIMAL(10,2) COMMENT '门票价格',
    opening_hours VARCHAR(100) COMMENT '营业时间',
    tips TEXT COMMENT '游览提示',
    rating DECIMAL(2,1) COMMENT '评分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_day_plan_id (day_plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点参观记录表';

CREATE TABLE IF NOT EXISTS meal_recommendation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    day_plan_id BIGINT NOT NULL COMMENT '每日行程ID',
    meal_type VARCHAR(20) NOT NULL COMMENT '餐食类型: breakfast/lunch/dinner',
    restaurant_name VARCHAR(200) NOT NULL COMMENT '餐厅名称',
    cuisine_type VARCHAR(100) COMMENT '菜系',
    address VARCHAR(300) COMMENT '地址',
    location VARCHAR(100) COMMENT '经纬度',
    avg_price DECIMAL(10,2) COMMENT '人均价格',
    rating DECIMAL(2,1) COMMENT '评分',
    reason TEXT COMMENT '推荐理由',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_day_plan_id (day_plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='餐饮推荐表';

-- =====================================================
-- Sample Data
-- =====================================================

-- 插入默认用户
INSERT INTO user_profile (user_id, username, nickname, email, status) VALUES
('user_001', 'demo_user', '旅行者', 'demo@example.com', 1)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 插入默认偏好
INSERT INTO user_travel_preference (
    user_id, 
    preference_type, 
    preference_name,
    favorite_destinations,
    budget_level,
    travel_style,
    interests,
    dietary_requirements,
    accommodation_type,
    transportation_preference,
    travel_companion,
    activity_level
) VALUES (
    'user_001',
    'default',
    '默认偏好',
    '杭州,北京,上海',
    'standard',
    '轻松漫游',
    '美食,自然,人文',
    '',
    '民宿',
    '高铁',
    'couple',
    'moderate'
) ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
