-- Roamly 通用旅行笔记模板。执行一次即可；不改动旧 inspiration/journey 数据。
CREATE TABLE IF NOT EXISTS travel_note (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL DEFAULT '',
  destination VARCHAR(128) NOT NULL DEFAULT '',
  note_type VARCHAR(24) NOT NULL DEFAULT 'inspiration' COMMENT 'inspiration/journey',
  source_type VARCHAR(24) NOT NULL DEFAULT 'manual' COMMENT 'manual/agent/import/copy',
  source_id BIGINT NULL,
  template_version INT NOT NULL DEFAULT 1,
  status VARCHAR(24) NOT NULL DEFAULT 'draft' COMMENT 'draft/planned/ongoing/completed/archived',
  visibility VARCHAR(24) NOT NULL DEFAULT 'private' COMMENT 'private/link',
  share_token VARCHAR(64) NULL,
  cover_url VARCHAR(1024) NOT NULL DEFAULT '',
  start_date DATE NULL,
  end_date DATE NULL,
  total_days INT NOT NULL DEFAULT 0,
  travelers INT NOT NULL DEFAULT 1,
  budget DECIMAL(12,2) NOT NULL DEFAULT 0,
  content_json JSON NOT NULL COMMENT '完整可复制笔记模板',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_share_token (share_token), KEY idx_note_user (user_id), KEY idx_note_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Roamly 可复制旅行笔记';

-- content_json 标准结构：
-- {"overview":{},"strategies":[],"budget":{"items":[]},"days":[{"day":1,"date":"","theme":"","budget":0,"weather":{},"items":[]}],"packing":[],"reminders":[],"reflections":{}}
-- 兼容 MySQL 5.7：以下两条请在确认字段尚未存在时各执行一次。
ALTER TABLE inspiration ADD COLUMN note_id BIGINT NULL COMMENT '关联通用旅行笔记';
ALTER TABLE journey ADD COLUMN note_id BIGINT NULL COMMENT '关联通用旅行笔记';
