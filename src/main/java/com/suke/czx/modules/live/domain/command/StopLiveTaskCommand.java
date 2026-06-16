package com.suke.czx.modules.live.domain.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 停止直播任务命令
 */
@Data
@Schema(description = "停止直播任务命令")
public class StopLiveTaskCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "任务ID不能为空")
    @Schema(description = "任务ID")
    private String taskId;
}
