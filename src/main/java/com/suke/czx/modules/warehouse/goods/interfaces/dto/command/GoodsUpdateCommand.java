package com.suke.czx.modules.warehouse.goods.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 货物更新命令
 */
@Data
@Schema(description = "货物更新命令")
public class GoodsUpdateCommand {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "货物ID不能为空")
    @Schema(description = "货物ID", required = true)
    private String goodsId;

    @NotBlank(message = "公仔名称不能为空")
    @Schema(description = "公仔名称", required = true)
    private String goodsName;

    @Schema(description = "公仔图片URL")
    private String goodsImage;

    @Schema(description = "高度(cm)")
    private BigDecimal height;

    @Schema(description = "重量(g)")
    private BigDecimal weight;

    @Schema(description = "类目")
    private String category;

    @NotBlank(message = "商品分类不能为空")
    @Schema(description = "商品分类: MALE-男款, FEMALE-女款, CHILDREN-儿童款", example = "MALE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productType;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;
}
