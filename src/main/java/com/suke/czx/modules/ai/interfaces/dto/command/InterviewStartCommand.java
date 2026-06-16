package com.suke.czx.modules.ai.interfaces.dto.command;

import com.suke.czx.modules.ai.domain.enums.InterviewDirectionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 开始面试命令
 */
@Data
@Schema(description = "开始面试请求")
public class InterviewStartCommand implements Serializable {

    @NotNull(message = "请选择面试方向")
    @Schema(description = "面试方向", example = "JAVA_BASIC")
    private InterviewDirectionEnum direction;
}
