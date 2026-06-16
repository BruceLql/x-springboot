package com.suke.czx.modules.ai.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 发送消息命令
 */
@Data
@Schema(description = "发送消息请求")
public class ChatCommand implements Serializable {

    @NotBlank(message = "会话ID不能为空")
    @Schema(description = "会话ID", example = "snowflake-id")
    private String sessionId;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容", example = "HashMap的底层原理是什么？")
    private String content;
}
