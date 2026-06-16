package com.suke.czx.modules.warehouse.goods.interfaces.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 货物信息VO
 */
@Data
@Schema(description = "货物信息VO")
public class GoodsVO implements Serializable {

    private static final long serialVersionUID = 1L;

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

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;
}
