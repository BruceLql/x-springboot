package com.suke.czx.modules.live.domain.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新直播任务命令
 */
@Data
@Schema(description = "更新直播任务命令")
public class UpdateLiveTaskCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "任务ID不能为空")
    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @NotBlank(message = "直播间URL不能为空")
    @Schema(description = "直播间URL")
    private String roomUrl;

    @Schema(description = "是否采集聊天数据: 0-否, 1-是")
    private Boolean collectChat;

    @Schema(description = "是否采集礼物数据: 0-否, 1-是")
    private Boolean collectGift;

    @Schema(description = "是否采集点赞数据: 0-否, 1-是")
    private Boolean collectLike;

    @Schema(description = "是否采集关注数据: 0-否, 1-是")
    private Boolean collectFollow;

    @Schema(description = "是否采集用户进入数据: 0-否, 1-是")
    private Boolean collectUser;

    @Schema(description = "最大监控时长(分钟), 最大480分钟")
    private Integer maxDuration;
}
