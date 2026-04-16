package com.suke.czx.modules.warehouse.supplier.interfaces.dto.query;

import com.suke.czx.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "供应商分页查询条件")
public class SupplierPageQuery extends BasePageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "供应商名称（模糊搜索）")
    private String supplierName;

    @Schema(description = "类型: FACTORY-源头厂家, DEALER-二道贩子")
    private String supplierType;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;
}
