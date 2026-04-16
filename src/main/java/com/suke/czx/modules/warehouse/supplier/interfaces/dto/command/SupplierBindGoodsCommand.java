package com.suke.czx.modules.warehouse.supplier.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 供应商绑定货物命令
 */
@Data
@Schema(description = "供应商绑定货物命令")
public class SupplierBindGoodsCommand {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "供应商ID不能为空")
    @Schema(description = "供应商ID", required = true)
    private String supplierId;

    @NotEmpty(message = "货物ID列表不能为空")
    @Schema(description = "货物ID列表", required = true)
    private List<String> goodsIds;
}
