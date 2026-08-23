USE travel_agent;
-- MySQL 5.7 兼容：确认字段不存在时执行一次
ALTER TABLE user_profile ADD COLUMN public_id CHAR(8) NULL COMMENT '8位公开用户ID';
UPDATE user_profile SET public_id = LPAD(CONV(CRC32(user_id),10,36),8,'0') WHERE public_id IS NULL OR public_id='';
ALTER TABLE user_profile ADD UNIQUE KEY uk_user_profile_public_id (public_id);
