package com.suke.czx.modules.live.interfaces.dto.query;

import com.suke.czx.common.base.BasePageQuery;
import com.suke.czx.modules.live.domain.enums.PlatformEnum;
import com.suke.czx.modules.live.domain.enums.TaskStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 直播任务分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "直播任务分页查询")
public class LiveTaskPageQuery extends BasePageQuery {

    @Schema(description = "平台: DOUYIN-抖音, TIKTOK-TikTok")
    private PlatformEnum platform;

    @Schema(description = "状态: PENDING-待监控, MONITORING-监控中, STOPPED-已停止, EXPIRED-已过期")
    private TaskStatusEnum taskStatus;

    @Schema(description = "直播间URL(模糊查询)")
    private String roomUrl;

    @Schema(description = "主播名称(模糊查询)")
    private String anchorName;
}
