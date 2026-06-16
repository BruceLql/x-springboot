package com.suke.czx.modules.warehouse.goods.interfaces.vo;

import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 货物信息+库存数量VO
 */
@Data
@Schema(description = "货物信息+库存数量VO")
@Builder
public class GoodsInventoryVO implements Serializable {

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

    //    以下为库存相关字段
    @Schema(description = "库存ID")
    private String inventoryId;

    @Schema(description = "可售库存 = real - withhold - occupy")
    private Integer quantity;

    @Version
    @Schema(description = "版本号(乐观锁)")
    private Integer version;

    @Schema(description = "租户ID")
    private Integer tenancyId;

}
