package com.suke.czx.modules.warehouse.supplier.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 供应商解绑货物命令
 */
@Data
@Schema(description = "供应商解绑货物命令")
public class SupplierUnbindGoodsCommand {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "供应商ID不能为空")
    @Schema(description = "供应商ID", required = true)
    private String supplierId;

    @NotBlank(message = "货物ID不能为空")
    @Schema(description = "货物ID", required = true)
    private String goodsId;
}
