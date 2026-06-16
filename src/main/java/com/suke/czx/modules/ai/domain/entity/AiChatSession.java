package com.suke.czx.modules.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.suke.czx.modules.ai.domain.enums.DifficultyLevelEnum;
import com.suke.czx.modules.ai.domain.enums.InterviewDirectionEnum;
import com.suke.czx.modules.ai.domain.enums.SessionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 会话实体（统一管理对话和面试会话）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_chat_session")
public class AiChatSession implements Serializable {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户ID */
    @TableField("user_id")
    private String userId;

    /** 会话标题 */
    private String title;

    /** 会话类型 */
    @TableField("session_type")
    private SessionTypeEnum sessionType;

    /** 面试方向（面试专用） */
    @TableField("interview_direction")
    private InterviewDirectionEnum interviewDirection;

    /** 当前难度（面试专用） */
    @TableField("difficulty_level")
    private DifficultyLevelEnum difficultyLevel;

    /** 会话状态 */
    private String status;

    /** 当前题号（面试专用，从1开始） */
    @TableField("question_index")
    private Integer questionIndex;

    /** 综合评分（面试专用） */
    @TableField("total_score")
    private BigDecimal totalScore;

    /** 面试报告JSON */
    @TableField("report")
    private String report;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
