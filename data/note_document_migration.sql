-- Roamly 旅行笔记（类飞书在线编辑器）。执行一次即可。
CREATE TABLE IF NOT EXISTS note_document (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL DEFAULT '未命名笔记',
  destination VARCHAR(128) NOT NULL DEFAULT '',
  cover_url VARCHAR(1024) NOT NULL DEFAULT '',
  visibility VARCHAR(24) NOT NULL DEFAULT 'private' COMMENT 'private/link',
  share_token VARCHAR(64) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'draft' COMMENT 'draft/published',
  source_social_note_id BIGINT NULL COMMENT '复制来源的 social_note.id',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_share_token (share_token),
  KEY idx_note_user (user_id),
  KEY idx_note_source_social (source_social_note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Roamly 笔记文档';

-- 已有 note_document 表补充完整内容和社区来源字段。
DROP PROCEDURE IF EXISTS migrate_note_document_columns;
DELIMITER $$
CREATE PROCEDURE migrate_note_document_columns()
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
CALL migrate_note_document_columns();
DROP PROCEDURE IF EXISTS migrate_note_document_columns;

CREATE TABLE IF NOT EXISTS note_block (
  id BIGINT NOT NULL AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  type VARCHAR(24) NOT NULL DEFAULT 'p' COMMENT 'h1/h2/p/list/todo/image/callout/code',
  text TEXT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  attrs_json VARCHAR(2048) NULL COMMENT '图片URL/todo勾选等附加属性(JSON)',
  PRIMARY KEY (id),
  KEY idx_block_doc (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Roamly 笔记内容块';
