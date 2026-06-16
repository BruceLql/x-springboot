package com.suke.czx.modules.live.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 直播间电商榜单实时数据（仅 Redis + WebSocket，不存 DB）
 */
@Data
@Schema(description = "直播间电商榜单")
public class LiveRankVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "榜单名称")
    private String rankName;

    @Schema(description = "榜单排序")
    private Integer rankOrder;

    @Schema(description = "榜单条目")
    private List<RankEntry> entries;

    @Schema(description = "更新时间戳")
    private Long updateTime;

    @Data
    public static class RankEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "排名")
        private Integer rank;

        @Schema(description = "名称/标题")
        private String name;

        @Schema(description = "分值")
        private String score;
    }
}
