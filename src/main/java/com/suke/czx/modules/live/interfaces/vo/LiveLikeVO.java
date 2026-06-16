package com.suke.czx.modules.live.interfaces.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直播点赞数据VO
 */
@Data
@Schema(description = "直播点赞数据VO")
public class LiveLikeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "点赞ID")
    private String likeId;

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "点赞数量")
    private Integer likeCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "点赞时间")
    private Date likeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;
}
