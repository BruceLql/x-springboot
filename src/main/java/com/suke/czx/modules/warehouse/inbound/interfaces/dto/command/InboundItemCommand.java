package com.suke.czx.modules.warehouse.inbound.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 入库明细命令
 */
@Data
@Schema(description = "入库明细命令")
public class InboundItemCommand {

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "入库数量")
    private Integer quantity;

    @Schema(description = "单位成本")
    private BigDecimal unitCost;
}
