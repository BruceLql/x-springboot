package com.suke.czx.modules.ai.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会话 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI会话")
public class ChatSessionVO implements Serializable {

    @Schema(description = "会话ID")
    private String id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "类型：CHAT/INTERVIEW")
    private String sessionType;

    @Schema(description = "面试方向")
    private String interviewDirection;

    @Schema(description = "难度级别")
    private String difficultyLevel;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "综合评分")
    private BigDecimal totalScore;

    @Schema(description = "面试报告JSON")
    private String report;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
