package com.suke.czx.modules.warehouse.inventory.domain.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 取消占用库存DTO（布款回滚）
 */
@Data
@Schema(description = "取消占用库存DTO")
public class CancelOccupyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "SKU ID")
    private String skuId;

    @Schema(description = "操作人ID")
    private String operatorId;

    @Schema(description = "租户ID")
    private Integer tenancyId;
}
