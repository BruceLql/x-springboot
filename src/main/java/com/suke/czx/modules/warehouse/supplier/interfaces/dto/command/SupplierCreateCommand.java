package com.suke.czx.modules.warehouse.supplier.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 供应商创建命令
 */
@Data
@Schema(description = "供应商创建命令")
public class SupplierCreateCommand {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "供应商名称不能为空")
    @Schema(description = "供应商名称", required = true)
    private String supplierName;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "联系方式")
    private String contactPhone;

    @NotBlank(message = "供应商类型不能为空")
    @Schema(description = "类型: FACTORY-源头厂家, DEALER-二道贩子", required = true)
    private String supplierType;
}
