package com.suke.czx.modules.ai.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "对话消息")
public class ChatMessageVO implements Serializable {

    @Schema(description = "消息ID")
    private String id;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "角色：USER/ASSISTANT/SYSTEM")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "引用来源JSON")
    private String sources;

    @Schema(description = "Token消耗")
    private Integer tokenCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
