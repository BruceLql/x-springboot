package com.suke.czx.modules.live.interfaces.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直播用户进入数据VO
 */
@Data
@Schema(description = "直播用户进入数据VO")
public class LiveUserEnterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "进入ID")
    private String enterId;

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "用户头像")
    private String userAvatar;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "进入时间")
    private Date enterTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;
}
