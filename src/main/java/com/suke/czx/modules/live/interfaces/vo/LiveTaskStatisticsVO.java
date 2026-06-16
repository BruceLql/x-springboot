package com.suke.czx.modules.live.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 直播任务统计VO
 */
@Data
@Schema(description = "直播任务统计VO")
public class LiveTaskStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "聊天数据数量")
    private Long chatCount;

    @Schema(description = "礼物数据数量")
    private Long giftCount;

    @Schema(description = "点赞数据数量")
    private Long likeCount;

    @Schema(description = "关注数据数量")
    private Long followCount;

    @Schema(description = "用户进入数据数量")
    private Long userEnterCount;

    @Schema(description = "下单数")
    private Long ecomOrderCount;

    @Schema(description = "独立聊天用户数")
    private Long chatUserCount;

    @Schema(description = "独立送礼用户数")
    private Long giftUserCount;

    @Schema(description = "礼物总数(所有礼物数量之和)")
    private Integer totalGiftCount;

    @Schema(description = "点赞总数(所有点赞数量之和)")
    private Integer totalLikeCount;
}
