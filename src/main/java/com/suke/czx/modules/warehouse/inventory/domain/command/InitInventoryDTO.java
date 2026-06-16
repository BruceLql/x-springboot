package com.suke.czx.modules.warehouse.inventory.domain.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 初始化货物库存DTO
 */
@Data
@Schema(description = "初始化货物库存DTO")
public class InitInventoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "租户ID")
    private Integer tenancyId;
}
