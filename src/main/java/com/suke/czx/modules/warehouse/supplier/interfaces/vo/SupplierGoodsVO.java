package com.suke.czx.modules.warehouse.supplier.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 供应商关联货物VO
 */
@Data
@Builder
@Schema(description = "供应商关联货物VO")
public class SupplierGoodsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关联ID")
    private Long id;

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "公仔名称")
    private String goodsName;

    @Schema(description = "公仔图片URL")
    private String goodsImage;

    @Schema(description = "高度(cm)")
    private BigDecimal height;

    @Schema(description = "重量(g)")
    private BigDecimal weight;

    @Schema(description = "类目")
    private String category;

    @Schema(description = "商品分类: MALE-男款, FEMALE-女款, CHILDREN-儿童款")
    private String productType;

    @Schema(description = "商品分类描述: 男款/女款/儿童款")
    private String productTypeDesc;
}
