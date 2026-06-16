package com.suke.czx.modules.warehouse.inventory.interfaces.dto.query;

import com.suke.czx.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SKU库存分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SKU库存分页查询条件")
public class SkuInventoryPageQuery extends BasePageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "SKU名称（模糊搜索）")
    private String skuName;

    @Schema(description = "SKU状态: 0-已取消, 1-待布款, 2-已布款, 3-布款回滚")
    private Integer status;
}
