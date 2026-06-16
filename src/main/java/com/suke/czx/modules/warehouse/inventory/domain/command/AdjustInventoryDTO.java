package com.suke.czx.modules.warehouse.inventory.domain.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 盘点调整库存DTO
 */
@Data
@Schema(description = "盘点调整库存DTO")
public class AdjustInventoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "调整数量(正数增加,负数减少)")
    private Integer adjustQuantity;

    @Schema(description = "操作人ID")
    private String operatorId;

    @Schema(description = "租户ID")
    private Integer tenancyId;
}
