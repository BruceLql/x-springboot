package com.suke.czx.modules.live.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直播礼物数据 - 领域实体
 */
@Data
@TableName("live_gift")
@Schema(description = "直播礼物数据")
public class LiveGift implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "礼物ID")
    private String giftId;

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "礼物名称")
    private String giftName;

    @Schema(description = "礼物数量")
    private Integer giftCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "送礼时间")
    private Date giftTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;
}
