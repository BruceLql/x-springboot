package com.suke.czx.modules.warehouse.inventory.interfaces.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 货物库存查询DTO
 */
@Data
@Schema(description = "货物库存查询DTO")
public class GoodsInventoryQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "货物ID")
    private String goodsId;
}
