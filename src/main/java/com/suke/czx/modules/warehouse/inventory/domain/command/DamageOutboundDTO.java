package com.suke.czx.modules.warehouse.inventory.domain.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报损出库DTO
 */
@Data
@Schema(description = "报损出库DTO")
public class DamageOutboundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "关联单号")
    private String orderNo;

    @Schema(description = "操作人ID")
    private String operatorId;

    @Schema(description = "租户ID")
    private Integer tenancyId;
}
