package com.suke.czx.modules.live.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 直播电商数据 VO（下单/购买/商品推广等）
 * <p>
 * content 字段为抖音返回的原始 JSON，结构不固定。
 * 解析成功后填充 businessType / rawJson，后续可根据实际字段扩展。
 */
@Data
@Schema(description = "直播电商数据")
public class LiveEcomDataVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息类型: SHOPPING-购物 / ECOM-电商通用 / RANK-排行榜 / SYNC-数据同步")
    private String ecomType;

    @Schema(description = "房间ID")
    private Long roomId;

    @Schema(description = "消息子类型（抖音内部 msgType）")
    private Long msgType;

    @Schema(description = "时间戳")
    private Long timestamp;

    @Schema(description = "业务类型标识（从 JSON 解析，如 order/promotion/product）")
    private String businessType;

    @Schema(description = "原始 JSON 数据")
    private String rawJson;
}
