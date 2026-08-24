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
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_share_token (share_token),
  KEY idx_note_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Roamly 笔记文档';

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
