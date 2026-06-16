package com.suke.czx.modules.warehouse.combination.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * SKU搭配创建命令
 */
@Data
@Schema(description = "SKU搭配创建命令")
public class CombinationCreateCommand {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "SKU名称不能为空")
    @Schema(description = "SKU名称(搭配名称)", required = true)
    private String skuName;

    @Schema(description = "SKU图片")
    private String skuImage;

    @NotNull(message = "预分配数量不能为空")
    @Schema(description = "预分配数量", required = true)
    private Integer preAllocateQuantity;

    @NotEmpty(message = "货物列表不能为空")
    @Schema(description = "货物ID列表", required = true)
    private List<String> goodsIds;
}
