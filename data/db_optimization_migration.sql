-- Roamly 数据库增量优化（兼容 MySQL 5.7/8.0）
-- 说明：不删除旧表、不重命名旧字段；先统一新功能写入 travel_note。
USE travel_agent;

-- 1) 通用旅行笔记：完整保存 Agent/用户编辑内容，可复制、迁移、分享
CREATE TABLE IF NOT EXISTS travel_note (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL DEFAULT '',
  destination VARCHAR(128) NOT NULL DEFAULT '',
  note_type VARCHAR(24) NOT NULL DEFAULT 'inspiration',
  source_type VARCHAR(24) NOT NULL DEFAULT 'manual',
  source_id BIGINT NULL,
  template_version INT NOT NULL DEFAULT 1,
  status VARCHAR(24) NOT NULL DEFAULT 'draft',
  visibility VARCHAR(24) NOT NULL DEFAULT 'private',
  share_token VARCHAR(64) NULL,
  cover_url VARCHAR(1024) NOT NULL DEFAULT '',
  start_date DATE NULL,
  end_date DATE NULL,
  total_days INT NOT NULL DEFAULT 0,
  travelers INT NOT NULL DEFAULT 1,
  budget DECIMAL(12,2) NOT NULL DEFAULT 0,
  content_json JSON NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_travel_note_share_token (share_token),
  KEY idx_travel_note_user_status (user_id, status),
  KEY idx_travel_note_destination (destination)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) 旧表只增加关联字段。若字段已经存在，请跳过对应语句。
ALTER TABLE inspiration ADD COLUMN note_id BIGINT NULL COMMENT '关联 travel_note';
ALTER TABLE journey ADD COLUMN note_id BIGINT NULL COMMENT '关联 travel_note';
ALTER TABLE travel_plan_history ADD COLUMN note_id BIGINT NULL COMMENT '关联 travel_note';
ALTER TABLE itinerary ADD COLUMN note_id BIGINT NULL COMMENT '关联 travel_note';

-- 3) 兼容旧数据库：补充关键查询索引（已存在时跳过）
ALTER TABLE inspiration ADD INDEX idx_inspiration_note_id (note_id);
ALTER TABLE journey ADD INDEX idx_journey_note_id (note_id);
ALTER TABLE travel_plan_history ADD INDEX idx_history_user_created (user_id, created_at);
ALTER TABLE agent_questionnaire ADD INDEX idx_questionnaire_user_status (user_id, status);

-- 4) 每日明细表补足唯一性，避免同一行程重复生成 Day 1
ALTER TABLE day_plan ADD UNIQUE KEY uk_itinerary_day (itinerary_id, day_number);
ALTER TABLE attraction_visit ADD KEY idx_attraction_day_order (day_plan_id, visit_order);
ALTER TABLE meal_recommendation ADD KEY idx_meal_day_type (day_plan_id, meal_type);

-- 5) 推荐的 content_json 规范（应用层校验，不强制迁移旧 JSON）
-- {
--   "overview": {"style":"", "pace":"", "walkKm":0, "comfort":5},
--   "strategies": [{"key":"weather","title":"天气适配","text":""}],
--   "budget": {"total":0,"items":[{"category":"meal","amount":0,"note":""}]},
--   "days": [{"day":1,"date":"","theme":"","budget":0,"weather":{},"items":[]}],
--   "alternatives": [], "reminders": [], "packing": [], "reflections": {}
-- }

-- 6) 可选外键（确认历史数据无脏关联后再执行）
-- ALTER TABLE inspiration ADD CONSTRAINT fk_inspiration_note FOREIGN KEY (note_id) REFERENCES travel_note(id) ON DELETE SET NULL;
-- ALTER TABLE journey ADD CONSTRAINT fk_journey_note FOREIGN KEY (note_id) REFERENCES travel_note(id) ON DELETE SET NULL;
-- ALTER TABLE travel_plan_history ADD CONSTRAINT fk_history_note FOREIGN KEY (note_id) REFERENCES travel_note(id) ON DELETE SET NULL;
