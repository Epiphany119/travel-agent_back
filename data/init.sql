-- Travel Agent Database Initialization Script
-- This script is executed when MySQL container starts for the first time

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS travel_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE travel_agent;

-- Create tables for travel agent (if needed in future)

-- Example: User sessions table
-- CREATE TABLE IF NOT EXISTS user_sessions (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     session_id VARCHAR(64) NOT NULL UNIQUE,
--     user_id VARCHAR(64),
--     context TEXT,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     INDEX idx_session_id (session_id),
--     INDEX idx_created_at (created_at)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Grant privileges (if using non-root user)
-- GRANT ALL PRIVILEGES ON travel_agent.* TO 'travel'@'%';
-- FLUSH PRIVILEGES;
