-- 直播监控任务表
CREATE TABLE `live_task` (
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `platform` varchar(10) NOT NULL COMMENT '平台: DOUYIN-抖音, TIKTOK-TikTok',
  `task_name` varchar(200) NOT NULL COMMENT '任务名称',
  `room_url` varchar(500) NOT NULL COMMENT '直播间URL',
  `room_id` varchar(100) DEFAULT NULL COMMENT '直播间ID(解析后)',
  `room_name` varchar(200) DEFAULT NULL COMMENT '直播间名称',
  `anchor_name` varchar(200) DEFAULT NULL COMMENT '主播名称',
  `task_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待监控, MONITORING-监控中, STOPPED-已停止, EXPIRED-已过期',
  `collect_chat` tinyint(1) DEFAULT 1 COMMENT '是否采集聊天数据: 0-否, 1-是',
  `collect_gift` tinyint(1) DEFAULT 1 COMMENT '是否采集礼物数据: 0-否, 1-是',
  `collect_like` tinyint(1) DEFAULT 0 COMMENT '是否采集点赞数据: 0-否, 1-是',
  `collect_follow` tinyint(1) DEFAULT 0 COMMENT '是否采集关注数据: 0-否, 1-是',
  `collect_user` tinyint(1) DEFAULT 0 COMMENT '是否采集用户进入数据: 0-否, 1-是',
  `max_duration` int DEFAULT 240 COMMENT '最大监控时长(分钟), 默认240分钟=4小时',
  `start_time` datetime DEFAULT NULL COMMENT '开始监控时间',
  `stop_time` datetime DEFAULT NULL COMMENT '停止时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间(开始时间+max_duration)',
  `error_msg` text COMMENT '错误信息',
  `tenant_id` int DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_platform` (`platform`),
  KEY `idx_status` (`task_status`),
  KEY `idx_expire_time` (`expire_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播监控任务表';

-- 直播聊天数据表
CREATE TABLE `live_chat` (
  `chat_id` varchar(64) NOT NULL COMMENT '聊天ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
  `user_nickname` varchar(200) DEFAULT NULL COMMENT '用户昵称',
  `user_avatar` varchar(500) DEFAULT NULL COMMENT '用户头像',
  `content` text COMMENT '聊天内容',
  `chat_time` datetime DEFAULT NULL COMMENT '聊天时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`chat_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_chat_time` (`chat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播聊天数据表';

-- 直播礼物数据表
CREATE TABLE `live_gift` (
  `gift_id` varchar(64) NOT NULL COMMENT '礼物ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
  `user_nickname` varchar(200) DEFAULT NULL COMMENT '用户昵称',
  `user_avatar` varchar(500) DEFAULT NULL COMMENT '用户头像',
  `gift_name` varchar(200) DEFAULT NULL COMMENT '礼物名称',
  `gift_count` int DEFAULT 1 COMMENT '礼物数量',
  `gift_time` datetime DEFAULT NULL COMMENT '送礼时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`gift_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_gift_time` (`gift_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播礼物数据表';

-- 直播点赞数据表
CREATE TABLE `live_like` (
  `like_id` varchar(64) NOT NULL COMMENT '点赞ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
  `user_nickname` varchar(200) DEFAULT NULL COMMENT '用户昵称',
  `user_avatar` varchar(500) DEFAULT NULL COMMENT '用户头像',
  `like_count` int DEFAULT 1 COMMENT '点赞数量',
  `like_time` datetime DEFAULT NULL COMMENT '点赞时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`like_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_like_time` (`like_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播点赞数据表';

-- 直播关注数据表
CREATE TABLE `live_follow` (
  `follow_id` varchar(64) NOT NULL COMMENT '关注ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
  `user_nickname` varchar(200) DEFAULT NULL COMMENT '用户昵称',
  `user_avatar` varchar(500) DEFAULT NULL COMMENT '用户头像',
  `follow_time` datetime DEFAULT NULL COMMENT '关注时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`follow_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_follow_time` (`follow_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播关注数据表';

-- 直播用户进入数据表
CREATE TABLE `live_user_enter` (
  `enter_id` varchar(64) NOT NULL COMMENT '进入ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
  `user_nickname` varchar(200) DEFAULT NULL COMMENT '用户昵称',
  `user_avatar` varchar(500) DEFAULT NULL COMMENT '用户头像',
  `enter_time` datetime DEFAULT NULL COMMENT '进入时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`enter_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_enter_time` (`enter_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播用户进入数据表';

-- =============================================
-- 菜单注册（需要手动执行）
-- =============================================
-- 父菜单：直播监控
INSERT INTO `sys_menu_new` (`parent_id`, `name`, `path`, `redirect`, `component`, `title`, `is_link`, `is_hide`, `is_keep_alive`, `is_affix`, `is_iframe`, `icon`, `roles`, `order_sort`, `disabled`)
VALUES (0, 'live', '/live', '/live/task', 'layout/routerView/parent', '直播监控', NULL, 0, 1, 0, 0, 'el-icon-video-play', 'admin', 5, 1);

-- 子菜单：直播任务（需要获取上面插入的menu_id作为parent_id）
-- 注意：请先执行父菜单插入，获取生成的menu_id后，再修改下面的parent_id值执行子菜单插入
# INSERT INTO `sys_menu_new` (`parent_id`, `name`, `path`, `redirect`, `component`, `title`, `is_link`, `is_hide`, `is_keep_alive`, `is_affix`, `is_iframe`, `icon`, `roles`, `order_sort`, `disabled`)
# VALUES ({parent_menu_id}, 'liveTask', '/liveTask', NULL, 'live/task/index', '直播任务', NULL, 0, 1, 0, 0, 'el-icon-video-camera', 'admin', 1, 1);
