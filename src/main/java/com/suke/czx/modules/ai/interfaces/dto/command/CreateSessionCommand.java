package com.suke.czx.modules.ai.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建会话命令
 */
@Data
@Schema(description = "创建会话请求")
public class CreateSessionCommand implements Serializable {

    @Schema(description = "会话标题（可选，不填则自动生成）", example = "Java基础学习")
    private String title;
}
