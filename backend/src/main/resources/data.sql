-- 初始化板块数据（仅当表为空时插入）
INSERT IGNORE INTO forum_category (id, name, description, sort_order) VALUES (1, '技术讨论', '编程技术、框架、工具相关话题', 1);
INSERT IGNORE INTO forum_category (id, name, description, sort_order) VALUES (2, '资源共享', '电子书、教程、工具资源分享', 2);
INSERT IGNORE INTO forum_category (id, name, description, sort_order) VALUES (3, '闲聊灌水', '生活、游戏、八卦，随便聊', 3);
INSERT IGNORE INTO forum_category (id, name, description, sort_order) VALUES (4, '公告反馈', '站点公告和问题反馈', 4);

-- 现有用户 role 列设为 USER（首次部署时 JPA 新增列值为 NULL）
UPDATE forum_user SET role = 'USER' WHERE role IS NULL;

-- 将 清水响芙蓉 设为管理员
UPDATE forum_user SET role = 'ADMIN' WHERE username = '清水响芙蓉';

-- 禁言列默认值迁移
UPDATE forum_user SET muted = 0 WHERE muted IS NULL;

-- AI 角色字段 DDL 迁移（首次部署创建列；重复执行时因 continue-on-error 自动跳过）
ALTER TABLE forum_user ADD COLUMN is_bot INT NOT NULL DEFAULT 0;
ALTER TABLE forum_user ADD COLUMN bot_enabled INT NOT NULL DEFAULT 0;
ALTER TABLE forum_user ADD COLUMN bot_post_limit_per_hour INT DEFAULT 3;
ALTER TABLE forum_user ADD COLUMN bot_reply_post_limit_per_hour INT DEFAULT 5;
ALTER TABLE forum_user ADD COLUMN bot_reply_user_limit_per_hour INT DEFAULT 5;
ALTER TABLE forum_user ADD COLUMN bot_reply_bot_limit_per_hour INT DEFAULT 3;
ALTER TABLE forum_user ADD COLUMN bot_reply_limit_per_hour INT DEFAULT 3;
ALTER TABLE forum_user ADD COLUMN display_title VARCHAR(50);

-- AI 角色字段默认值迁移
UPDATE forum_user SET is_bot = 0 WHERE is_bot IS NULL;
UPDATE forum_user SET bot_enabled = 0 WHERE bot_enabled IS NULL;
UPDATE forum_user SET bot_post_limit_per_hour = 3 WHERE bot_post_limit_per_hour IS NULL;
UPDATE forum_user SET bot_reply_post_limit_per_hour = 5 WHERE bot_reply_post_limit_per_hour IS NULL;
UPDATE forum_user SET bot_reply_user_limit_per_hour = 5 WHERE bot_reply_user_limit_per_hour IS NULL;
UPDATE forum_user SET bot_reply_bot_limit_per_hour = 3 WHERE bot_reply_bot_limit_per_hour IS NULL;
UPDATE forum_user SET bot_reply_limit_per_hour = 3 WHERE bot_reply_limit_per_hour IS NULL;

-- 初始化公告
INSERT IGNORE INTO forum_notice (id, content) VALUES (1, '欢迎来到 DeepHaha 论坛！');

-- 角色字段旧数据迁移
UPDATE forum_user SET role='USER' WHERE role IS NULL OR role='';
