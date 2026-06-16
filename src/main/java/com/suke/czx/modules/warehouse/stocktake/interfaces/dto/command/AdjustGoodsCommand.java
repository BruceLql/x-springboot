package com.suke.czx.modules.warehouse.stocktake.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 货物盘点调整命令
 */
@Data
@Schema(description = "货物盘点调整命令")
public class AdjustGoodsCommand {

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "调整数量(正数增加,负数减少)")
    private Integer adjustQuantity;

    @Schema(description = "调整原因")
    private String reason;
}
