package com.suke.czx.modules.live.interfaces.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直播任务VO
 */
@Data
@Schema(description = "直播任务VO")
public class LiveTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "平台: DOUYIN-抖音, TIKTOK-TikTok")
    private String platform;

    @Schema(description = "平台描述")
    private String platformDesc;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "直播间URL")
    private String roomUrl;

    @Schema(description = "直播间ID")
    private String roomId;

    @Schema(description = "直播间名称")
    private String roomName;

    @Schema(description = "主播名称")
    private String anchorName;

    @Schema(description = "状态: PENDING-待监控, MONITORING-监控中, STOPPED-已停止, EXPIRED-已过期")
    private String taskStatus;

    @Schema(description = "任务状态描述")
    private String taskStatusDesc;

    @Schema(description = "是否采集聊天数据")
    private Boolean collectChat;

    @Schema(description = "是否采集礼物数据")
    private Boolean collectGift;

    @Schema(description = "是否采集点赞数据")
    private Boolean collectLike;

    @Schema(description = "是否采集关注数据")
    private Boolean collectFollow;

    @Schema(description = "是否采集用户进入数据")
    private Boolean collectUser;

    @Schema(description = "最大监控时长(分钟)")
    private Integer maxDuration;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "开始监控时间")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "停止时间")
    private Date stopTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "过期时间")
    private Date expireTime;

    @Schema(description = "错误信息")
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "统计数据汇总")
    private LiveTaskStatisticsVO statistics;
}
