package com.suke.czx.modules.ai.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 面试会话操作命令（跳过、结束等）
 */
@Data
@Schema(description = "面试会话操作")
public class InterviewSessionCommand implements Serializable {

    @NotBlank(message = "会话ID不能为空")
    @Schema(description = "面试会话ID", example = "e8acd14986ce162d8e67643032eb1c0f")
    private String sessionId;
}
