USE travel_agent;

-- 社区层：笔记、评论、点赞、收藏、好友申请。公开内容无需互加好友即可阅读。
CREATE TABLE IF NOT EXISTS social_note (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id VARCHAR(64) NOT NULL, travel_note_id BIGINT NULL,
  title VARCHAR(200) NOT NULL DEFAULT '', content TEXT NOT NULL, cover_url VARCHAR(1024) NOT NULL DEFAULT '',
  visibility VARCHAR(16) NOT NULL DEFAULT 'public', status VARCHAR(16) NOT NULL DEFAULT 'published',
  like_count INT NOT NULL DEFAULT 0, comment_count INT NOT NULL DEFAULT 0, favorite_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_social_note_feed(status, created_at), KEY idx_social_note_user(user_id), KEY idx_social_note_travel(travel_note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
