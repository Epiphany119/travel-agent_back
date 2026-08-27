USE travel_agent;

-- 社区层：笔记、评论、点赞、收藏、好友申请。公开内容无需互加好友即可阅读。
CREATE TABLE IF NOT EXISTS social_note (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id VARCHAR(64) NOT NULL, travel_note_id BIGINT NULL,
  title VARCHAR(200) NOT NULL DEFAULT '', content TEXT NOT NULL, cover_url VARCHAR(1024) NOT NULL DEFAULT '',
  destination VARCHAR(128) NOT NULL DEFAULT '', tags JSON NULL,
  author_name VARCHAR(100) NOT NULL DEFAULT '', author_avatar VARCHAR(1024) NOT NULL DEFAULT '',
  visibility VARCHAR(16) NOT NULL DEFAULT 'public', status VARCHAR(16) NOT NULL DEFAULT 'published',
  like_count INT NOT NULL DEFAULT 0, comment_count INT NOT NULL DEFAULT 0, favorite_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_social_note_feed(status, created_at), KEY idx_social_note_user(user_id), KEY idx_social_note_travel(travel_note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 探索页卡片扩展字段（已有环境执行本段即可）
-- 兼容 MySQL 5.7/8.0：旧版本不支持 ADD COLUMN IF NOT EXISTS
DROP PROCEDURE IF EXISTS migrate_social_note_columns;
DELIMITER $$
CREATE PROCEDURE migrate_social_note_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='destination') THEN
    ALTER TABLE social_note ADD COLUMN destination VARCHAR(128) NOT NULL DEFAULT '' COMMENT '目的地/城市';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='tags') THEN
    ALTER TABLE social_note ADD COLUMN tags JSON NULL COMMENT '标签数组';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='author_name') THEN
    ALTER TABLE social_note ADD COLUMN author_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '发布时作者昵称快照';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='author_avatar') THEN
    ALTER TABLE social_note ADD COLUMN author_avatar VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '发布时作者头像快照';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='social_note' AND index_name='idx_social_note_destination') THEN
    ALTER TABLE social_note ADD INDEX idx_social_note_destination (destination);
  END IF;
END$$
DELIMITER ;
CALL migrate_social_note_columns();
DROP PROCEDURE IF EXISTS migrate_social_note_columns;
CREATE TABLE IF NOT EXISTS social_comment (
  id BIGINT NOT NULL AUTO_INCREMENT, note_id BIGINT NOT NULL, user_id VARCHAR(64) NOT NULL, parent_id BIGINT NULL,
  content VARCHAR(1000) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_comment_note(note_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS social_reaction (
  id BIGINT NOT NULL AUTO_INCREMENT, note_id BIGINT NOT NULL, user_id VARCHAR(64) NOT NULL, reaction_type VARCHAR(16) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(id), UNIQUE KEY uk_reaction(note_id,user_id,reaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS social_friend_request (
  id BIGINT NOT NULL AUTO_INCREMENT, requester_id VARCHAR(64) NOT NULL, receiver_id VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'pending', message VARCHAR(200) NOT NULL DEFAULT '', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), UNIQUE KEY uk_friend_request(requester_id,receiver_id), KEY idx_friend_receiver(receiver_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
