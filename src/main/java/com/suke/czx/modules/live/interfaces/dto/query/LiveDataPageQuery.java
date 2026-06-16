package com.suke.czx.modules.live.interfaces.dto.query;

import com.suke.czx.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 直播数据分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "直播数据分页查询")
public class LiveDataPageQuery extends BasePageQuery {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户昵称(模糊查询)")
    private String userNickname;

    @Schema(description = "聊天内容(模糊查询)")
    private String content;

    @Schema(description = "电商业务类型: ORDER/PRODUCT_ON/PRODUCT_OFF/PROMOTION/CART/OTHER")
    private String businessType;
}
