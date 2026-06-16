package com.suke.czx.modules.warehouse.outbound.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 报损出库命令
 */
@Data
@Schema(description = "报损出库命令")
public class DamageOutboundCommand {

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "出库数量")
    private Integer quantity;

    @Schema(description = "报损原因")
    private String reason;
}
