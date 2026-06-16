package com.suke.czx.modules.live.domain.command;

import com.suke.czx.modules.live.domain.enums.PlatformEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 创建直播任务命令
 */
@Data
@Schema(description = "创建直播任务命令")
public class CreateLiveTaskCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "平台不能为空")
    @Schema(description = "平台: DOUYIN-抖音, TIKTOK-TikTok")
    private PlatformEnum platform;

    @NotBlank(message = "任务名称不能为空")
    @Schema(description = "任务名称")
    private String taskName;

    @NotBlank(message = "直播间URL不能为空")
    @Schema(description = "直播间URL")
    private String roomUrl;

    @Schema(description = "是否采集聊天数据: 0-否, 1-是, 默认是")
    private Boolean collectChat = true;

    @Schema(description = "是否采集礼物数据: 0-否, 1-是, 默认是")
    private Boolean collectGift = true;

    @Schema(description = "是否采集点赞数据: 0-否, 1-是, 默认否")
    private Boolean collectLike = false;

    @Schema(description = "是否采集关注数据: 0-否, 1-是, 默认否")
    private Boolean collectFollow = false;

    @Schema(description = "是否采集用户进入数据: 0-否, 1-是, 默认否")
    private Boolean collectUser = false;

    @Schema(description = "是否采集电商数据(下单/上下架/促销): 0-否, 1-是, 默认是")
    private Boolean collectEcom = true;

    @Schema(description = "最大监控时长(分钟), 默认240分钟=4小时, 最大480分钟")
    private Integer maxDuration = 240;
}
