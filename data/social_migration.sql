USE travel_agent;

-- Roamly 社区与版权治理迁移（最新版）
-- 说明：复制关系只使用 social_note.id 追踪；会话表不参与版权发布判断。
-- 请在 user_profile、user_travel_preference、note_document 所在数据库执行。

CREATE TABLE IF NOT EXISTS social_note (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  travel_note_id BIGINT NULL,
  source_note_id BIGINT NULL COMMENT '复制来源的 social_note.id；原创帖子为空',
  title VARCHAR(200) NOT NULL DEFAULT '',
  content TEXT NOT NULL,
  cover_url VARCHAR(1024) NOT NULL DEFAULT '',
  destination VARCHAR(128) NOT NULL DEFAULT '',
  tags JSON NULL,
  state_code CHAR(8) NULL COMMENT '当前公开版本状态码，8 位小写字母数字',
  author_name VARCHAR(100) NOT NULL DEFAULT '',
  author_avatar VARCHAR(1024) NOT NULL DEFAULT '',
  visibility VARCHAR(16) NOT NULL DEFAULT 'public' COMMENT 'public/private',
  status VARCHAR(24) NOT NULL DEFAULT 'published' COMMENT 'published/pending_review/rejected',
  moderation_status VARCHAR(24) NOT NULL DEFAULT 'approved' COMMENT 'approved/pending_review/rejected',
  moderation_score DECIMAL(6,5) NULL COMMENT 'AI 与来源帖子的相似度 0~1',
  moderation_reason VARCHAR(1000) NOT NULL DEFAULT '',
  review_required TINYINT(1) NOT NULL DEFAULT 0,
  report_count INT NOT NULL DEFAULT 0,
  like_count INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  favorite_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_social_note_feed(status, visibility, created_at),
  KEY idx_social_note_user(user_id),
  KEY idx_social_note_source(source_note_id),
  KEY idx_social_note_travel(travel_note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Roamly 社区帖子';

-- 给已经存在的 social_note 补充新字段。
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
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='source_note_id') THEN
    ALTER TABLE social_note ADD COLUMN source_note_id BIGINT NULL COMMENT '复制来源的 social_note.id' AFTER travel_note_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='state_code') THEN
    ALTER TABLE social_note ADD COLUMN state_code CHAR(8) NULL COMMENT '当前公开版本状态码' AFTER tags;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='author_name') THEN
    ALTER TABLE social_note ADD COLUMN author_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '发布时作者昵称快照';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='author_avatar') THEN
    ALTER TABLE social_note ADD COLUMN author_avatar VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '发布时作者头像快照';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='moderation_status') THEN
    ALTER TABLE social_note ADD COLUMN moderation_status VARCHAR(24) NOT NULL DEFAULT 'approved' COMMENT '版权审核状态';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='moderation_score') THEN
    ALTER TABLE social_note ADD COLUMN moderation_score DECIMAL(6,5) NULL COMMENT 'AI 相似度';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='moderation_reason') THEN
    ALTER TABLE social_note ADD COLUMN moderation_reason VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '审核说明';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='review_required') THEN
    ALTER TABLE social_note ADD COLUMN review_required TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要平台人工审核';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='social_note' AND column_name='report_count') THEN
    ALTER TABLE social_note ADD COLUMN report_count INT NOT NULL DEFAULT 0 COMMENT '举报次数';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='social_note' AND index_name='idx_social_note_destination') THEN
    ALTER TABLE social_note ADD INDEX idx_social_note_destination (destination);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='social_note' AND index_name='idx_social_note_source') THEN
    ALTER TABLE social_note ADD INDEX idx_social_note_source (source_note_id);
  END IF;
END$$
DELIMITER ;
CALL migrate_social_note_columns();
DROP PROCEDURE IF EXISTS migrate_social_note_columns;

-- 历史帖子补齐 8 位状态码；UUID 去掉连字符后取前 8 位，符合小写字母数字格式。
UPDATE social_note
SET state_code=LOWER(SUBSTRING(REPLACE(UUID(),'-',''),1,8))
WHERE state_code IS NULL OR state_code='';

CREATE TABLE IF NOT EXISTS social_comment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  note_id BIGINT NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  parent_id BIGINT NULL,
  content VARCHAR(1000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_comment_note(note_id, created_at),
  KEY idx_comment_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区评论';

CREATE TABLE IF NOT EXISTS social_reaction (
  id BIGINT NOT NULL AUTO_INCREMENT,
  note_id BIGINT NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  reaction_type VARCHAR(16) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reaction(note_id,user_id,reaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区点赞收藏';

CREATE TABLE IF NOT EXISTS social_friend_request (
  id BIGINT NOT NULL AUTO_INCREMENT,
  requester_id VARCHAR(64) NOT NULL,
  receiver_id VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'pending',
  message VARCHAR(200) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_friend_request(requester_id,receiver_id),
  KEY idx_friend_receiver(receiver_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请';

-- 复制与协作版本日志：source_note_id 是来源帖子 ID，不记录会话号。
CREATE TABLE IF NOT EXISTS social_note_revision (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source_note_id BIGINT NOT NULL COMMENT '来源 social_note.id',
  owner_id VARCHAR(64) NOT NULL COMMENT '来源帖子作者',
  contributor_id VARCHAR(64) NOT NULL COMMENT '复制者或协作者',
  private_note_id BIGINT NULL COMMENT '复制后生成的 note_document.id',
  published_note_id BIGINT NULL COMMENT '该副本发布后生成的新 social_note.id',
  revision_code CHAR(8) NOT NULL COMMENT '本次版本 8 位小写字母数字编号',
  revision_no INT NOT NULL DEFAULT 1 COMMENT '同一来源帖子的内部递增版本号',
  source_type VARCHAR(16) NOT NULL DEFAULT 'invite' COMMENT 'copy/invite',
  status VARCHAR(16) NOT NULL DEFAULT 'requested' COMMENT 'requested/approved/submitted/rejected/merged/archived',
  title VARCHAR(200) NOT NULL DEFAULT '',
  content TEXT NOT NULL,
  cover_url VARCHAR(1024) NOT NULL DEFAULT '',
  destination VARCHAR(128) NOT NULL DEFAULT '',
  tags JSON NULL,
  message VARCHAR(1000) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  reviewed_at TIMESTAMP NULL,
  merged_at TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_revision_code(revision_code),
  UNIQUE KEY uk_source_contributor_revision(source_note_id,contributor_id,revision_no,source_type),
  KEY idx_revision_source_status(source_note_id,status,updated_at),
  KEY idx_revision_contributor(contributor_id,status),
  KEY idx_revision_private_note(private_note_id),
  KEY idx_revision_published_note(published_note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区笔记复制与 PR 版本日志';

-- 用户信誉分：新用户默认 100 分。
CREATE TABLE IF NOT EXISTS social_user_reputation (
  user_id VARCHAR(64) NOT NULL,
  score INT NOT NULL DEFAULT 100,
  report_count INT NOT NULL DEFAULT 0 COMMENT '被举报次数',
  confirmed_report_count INT NOT NULL DEFAULT 0 COMMENT '被确认的侵权举报次数',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  CONSTRAINT chk_social_reputation_score CHECK (score >= 0 AND score <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区用户信誉分';

CREATE TABLE IF NOT EXISTS social_reputation_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  note_id BIGINT NULL,
  report_id BIGINT NULL,
  event_type VARCHAR(24) NOT NULL COMMENT 'copyright_report/restore/manual',
  delta INT NOT NULL,
  score_before INT NOT NULL,
  score_after INT NOT NULL,
  reason VARCHAR(500) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reputation_report_event(report_id,event_type),
  KEY idx_reputation_user(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信誉分变更流水';

-- 版权举报：一个举报人对同一帖子同一类型只能提交一次，避免重复扣分。
CREATE TABLE IF NOT EXISTS social_note_report (
  id BIGINT NOT NULL AUTO_INCREMENT,
  note_id BIGINT NOT NULL COMMENT '被举报帖子 ID',
  source_note_id BIGINT NOT NULL COMMENT '举报人主张的来源帖子 ID',
  reporter_id VARCHAR(64) NOT NULL COMMENT '举报人，通常为来源帖子作者',
  note_owner_id VARCHAR(64) NOT NULL COMMENT '被举报帖子作者',
  report_type VARCHAR(24) NOT NULL DEFAULT 'copyright',
  reason VARCHAR(1000) NOT NULL DEFAULT '',
  evidence VARCHAR(2000) NOT NULL DEFAULT '',
  status VARCHAR(24) NOT NULL DEFAULT 'pending' COMMENT 'pending/ai_confirmed/manual_review/dismissed/confirmed',
  ai_engine VARCHAR(64) NOT NULL DEFAULT 'agent-guard-v1',
  ai_similarity DECIMAL(6,5) NULL,
  ai_decision VARCHAR(24) NOT NULL DEFAULT 'pending' COMMENT 'allow/auto_reject/manual_review',
  ai_reason VARCHAR(1000) NOT NULL DEFAULT '',
  human_notified_at TIMESTAMP NULL,
  reviewed_by VARCHAR(64) NULL,
  reviewed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reporter_note_type(reporter_id,note_id,report_type),
  KEY idx_report_note_status(note_id,status,created_at),
  KEY idx_report_queue(status,created_at),
  KEY idx_report_source(source_note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区版权举报';

-- Agent 版权识别流水：发布前、举报后都写一条，便于人工复核和追责。
CREATE TABLE IF NOT EXISTS social_note_moderation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  note_id BIGINT NOT NULL,
  source_note_id BIGINT NULL,
  user_id VARCHAR(64) NOT NULL,
  trigger_type VARCHAR(24) NOT NULL COMMENT 'publish/report/manual',
  ai_engine VARCHAR(64) NOT NULL DEFAULT 'agent-guard-v1',
  similarity_score DECIMAL(6,5) NULL,
  decision VARCHAR(24) NOT NULL COMMENT 'allow/auto_reject/manual_review',
  status VARCHAR(24) NOT NULL DEFAULT 'completed' COMMENT 'completed/pending/approved/rejected',
  reason VARCHAR(1000) NOT NULL DEFAULT '',
  human_notified TINYINT(1) NOT NULL DEFAULT 0,
  reviewed_by VARCHAR(64) NULL,
  reviewed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_moderation_note(note_id,created_at),
  KEY idx_moderation_queue(status,human_notified,created_at),
  KEY idx_moderation_source(source_note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 版权识别流水';

-- 平台人工审核记录；信誉分低于 60 或 Agent 判定相似度较高时进入队列。
CREATE TABLE IF NOT EXISTS social_platform_review (
  id BIGINT NOT NULL AUTO_INCREMENT,
  note_id BIGINT NOT NULL,
  moderation_id BIGINT NULL,
  report_id BIGINT NULL,
  applicant_id VARCHAR(64) NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  reason VARCHAR(1000) NOT NULL DEFAULT '',
  reviewer_id VARCHAR(64) NULL,
  reviewed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_platform_review_queue(status,created_at),
  KEY idx_platform_review_note(note_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区平台人工审核队列';

-- 为已有用户和历史帖子初始化信誉分；已有分数不会被覆盖。
INSERT INTO social_user_reputation(user_id)
SELECT user_id FROM user_profile WHERE user_id IS NOT NULL AND user_id <> ''
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);
INSERT INTO social_user_reputation(user_id)
SELECT DISTINCT user_id FROM social_note WHERE user_id IS NOT NULL AND user_id <> ''
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

-- note_document 是个人笔记模块的表；此处只补充复制来源字段，便于发布时带出 source_note_id。
CREATE TABLE IF NOT EXISTS note_document (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL DEFAULT '未命名笔记',
  destination VARCHAR(128) NOT NULL DEFAULT '',
  cover_url VARCHAR(1024) NOT NULL DEFAULT '',
  visibility VARCHAR(24) NOT NULL DEFAULT 'private',
  share_token VARCHAR(64) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'draft',
  theme_json TEXT NULL,
  content LONGTEXT NULL,
  source_social_note_id BIGINT NULL COMMENT '复制来源的 social_note.id',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_share_token(share_token),
  KEY idx_note_user(user_id),
  KEY idx_note_source_social(source_social_note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Roamly 私人笔记文档';

DROP PROCEDURE IF EXISTS migrate_note_document_source;
DELIMITER $$
CREATE PROCEDURE migrate_note_document_source()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='note_document' AND column_name='content') THEN
    ALTER TABLE note_document ADD COLUMN content LONGTEXT NULL COMMENT '完整 Markdown/HTML 内容';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='note_document' AND column_name='theme_json') THEN
    ALTER TABLE note_document ADD COLUMN theme_json TEXT NULL COMMENT '笔记主题 JSON';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='note_document' AND column_name='source_social_note_id') THEN
    ALTER TABLE note_document ADD COLUMN source_social_note_id BIGINT NULL COMMENT '复制来源的 social_note.id';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='note_document' AND index_name='idx_note_source_social') THEN
    ALTER TABLE note_document ADD INDEX idx_note_source_social (source_social_note_id);
  END IF;
END$$
DELIMITER ;
CALL migrate_note_document_source();
DROP PROCEDURE IF EXISTS migrate_note_document_source;
