package com.suke.czx.modules.live.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.suke.czx.modules.live.domain.enums.PlatformEnum;
import com.suke.czx.modules.live.domain.enums.TaskStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直播监控任务 - 领域实体
 */
@Data
@TableName("live_task")
@Schema(description = "直播监控任务")
public class LiveTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "平台: DOUYIN-抖音, TIKTOK-TikTok")
    private PlatformEnum platform;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "直播间URL")
    private String roomUrl;

    @Schema(description = "直播间ID(解析后)")
    private String roomId;

    @Schema(description = "直播间名称")
    private String roomName;

    @Schema(description = "主播名称")
    private String anchorName;

    @Schema(description = "状态: PENDING-待监控, MONITORING-监控中, STOPPED-已停止, EXPIRED-已过期")
    private TaskStatusEnum taskStatus;

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

    @Schema(description = "是否采集电商数据: 0-否, 1-是")
    private Boolean collectEcom;

    @Schema(description = "最大监控时长(分钟), 默认240分钟=4小时")
    private Integer maxDuration;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "开始监控时间")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "停止时间")
    private Date stopTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "过期时间(开始时间+max_duration)")
    private Date expireTime;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "租户ID")
    private Integer tenantId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private Date updateTime;
}
