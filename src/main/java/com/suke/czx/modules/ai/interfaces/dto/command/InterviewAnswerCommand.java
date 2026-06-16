package com.suke.czx.modules.ai.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 面试回答命令
 */
@Data
@Schema(description = "面试回答请求")
public class InterviewAnswerCommand implements Serializable {

    @NotBlank(message = "会话ID不能为空")
    @Schema(description = "面试会话ID")
    private String sessionId;

    @NotBlank(message = "回答内容不能为空")
    @Schema(description = "回答内容")
    private String answer;
}
