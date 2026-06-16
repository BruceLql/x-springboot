package com.suke.czx.modules.warehouse.combination.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * SKU搭配关联货物项VO
 */
@Data
@Schema(description = "SKU搭配关联货物项VO")
public class CombinationGoodsItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "公仔名称")
    private String goodsName;

    @Schema(description = "公仔图片URL")
    private String goodsImage;
}
