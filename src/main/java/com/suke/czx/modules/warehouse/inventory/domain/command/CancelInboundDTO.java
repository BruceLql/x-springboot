package com.suke.czx.modules.warehouse.inventory.domain.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 取消入库DTO（回滚入库库存）
 */
@Data
@Schema(description = "取消入库DTO")
public class CancelInboundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "关联批次号")
    private String batchNo;

    @Schema(description = "操作人ID")
    private String operatorId;

    @Schema(description = "租户ID")
    private Integer tenancyId;
}
