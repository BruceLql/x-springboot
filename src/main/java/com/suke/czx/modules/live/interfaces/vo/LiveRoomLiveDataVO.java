package com.suke.czx.modules.live.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 直播间实时数据VO（在线人数+排行榜）
 */
@Data
@Schema(description = "直播间实时数据VO")
public class LiveRoomLiveDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "在线人数（短文本展示，如 1.2万）")
    private String onlineDisplay;

    @Schema(description = "在线人数总数")
    private Long onlineTotal;

    @Schema(description = "在线人数更新时间(毫秒时间戳)")
    private Long onlineUpdateTime;

    @Schema(description = "排行榜数据")
    private List<RankItem> ranks = new ArrayList<>();

    @Schema(description = "排行榜更新时间(毫秒时间戳)")
    private Long rankUpdateTime;

    @Data
    @Schema(description = "排行榜成员")
    public static class RankItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "排名")
        private Integer rank;

        @Schema(description = "用户ID")
        private String userId;

        @Schema(description = "用户昵称")
        private String userNickname;

        @Schema(description = "用户头像")
        private String userAvatar;

        @Schema(description = "积分文本")
        private String scoreStr;
    }
}
