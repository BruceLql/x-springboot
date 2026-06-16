package com.suke.czx.modules.warehouse.inventory.interfaces.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量货物库存查询DTO
 */
@Data
@Schema(description = "批量货物库存查询DTO")
public class BatchGoodsInventoryQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "货物ID列表")
    private List<String> goodsIds;
}
