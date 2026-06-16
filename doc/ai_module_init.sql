-- =====================================================
-- AI 模块初始化 SQL
-- 创建知识库、会话、消息三张表
-- =====================================================

-- 1. 知识文档元数据表
CREATE TABLE IF NOT EXISTS `ai_knowledge` (
    `id`          VARCHAR(64)  NOT NULL COMMENT '主键（Snowflake分布式ID）',
    `title`       VARCHAR(200) NOT NULL COMMENT '文档标题',
    `category`    VARCHAR(50)  DEFAULT NULL COMMENT '分类（如01-Java基础）',
    `file_path`   VARCHAR(500) DEFAULT NULL COMMENT '文件路径',
    `status`      VARCHAR(20)  DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DELETED',
    `chunk_count` INT          DEFAULT 0 COMMENT '分块数量',
    `word_count`  INT          DEFAULT 0 COMMENT '总字数',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识文档元数据';

-- 2. AI 会话表（统一管理对话和面试）
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `id`                  VARCHAR(64)  NOT NULL COMMENT '主键（Snowflake分布式ID）',
    `user_id`             VARCHAR(64)  NOT NULL COMMENT '用户ID',
    `title`               VARCHAR(200) DEFAULT '新对话' COMMENT '会话标题',
    `session_type`        VARCHAR(20)  NOT NULL DEFAULT 'CHAT' COMMENT '类型: CHAT/INTERVIEW',
    `interview_direction` VARCHAR(50)  DEFAULT NULL COMMENT '面试方向',
    `difficulty_level`    VARCHAR(20)  DEFAULT 'JUNIOR' COMMENT '难度: JUNIOR/MIDDLE/SENIOR',
    `status`              VARCHAR(20)  DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/CLOSED',
    `question_index`      INT          DEFAULT 0 COMMENT '当前题号（从1开始）',
    `total_score`         DECIMAL(5,2) DEFAULT NULL COMMENT '综合评分',
    `report`              TEXT         DEFAULT NULL COMMENT '面试报告JSON',
    `create_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_type`  (`user_id`, `session_type`),
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话表';

-- 3. 对话消息表
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id`          VARCHAR(64)  NOT NULL COMMENT '主键（Snowflake分布式ID）',
    `session_id`  VARCHAR(64)  NOT NULL COMMENT '会话ID',
    `role`        VARCHAR(20)  NOT NULL COMMENT '角色: USER/ASSISTANT/SYSTEM',
    `content`     TEXT         DEFAULT NULL COMMENT '消息内容',
    `sources`     JSON         DEFAULT NULL COMMENT '引用来源JSON',
    `token_count` INT          DEFAULT 0 COMMENT 'Token消耗数',
    `metadata`    JSON         DEFAULT NULL COMMENT '扩展元数据',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_session_role` (`session_id`, `role`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';
