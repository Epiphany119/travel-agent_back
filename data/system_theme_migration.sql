-- 用户系统界面主题配置。执行一次即可。
ALTER TABLE user_travel_preference
  ADD COLUMN system_theme_json VARCHAR(512) NOT NULL DEFAULT '{"fg":"#1D2B27","bg":"#F7F3EA","accent":"#164E42"}' COMMENT '系统界面主题 JSON';
