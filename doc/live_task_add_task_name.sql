-- 直播任务表增加任务名称字段
ALTER TABLE `live_task` ADD COLUMN `task_name` varchar(200) NOT NULL COMMENT '任务名称' AFTER `platform`;

-- 为已有数据生成默认任务名称（基于直播间URL）
UPDATE `live_task` SET `task_name` = CONCAT('任务_', SUBSTRING(task_id, 1, 8)) WHERE `task_name` IS NULL OR `task_name` = '';
