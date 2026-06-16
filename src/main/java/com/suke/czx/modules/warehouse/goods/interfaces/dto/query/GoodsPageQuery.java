package com.suke.czx.modules.warehouse.goods.interfaces.dto.query;

import com.suke.czx.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 货物分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "货物分页查询条件")
public class GoodsPageQuery extends BasePageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "公仔名称（模糊搜索）")
    private String goodsName;

    @Schema(description = "类目")
    private String category;

    @Schema(description = "商品分类: MALE-男款, FEMALE-女款, CHILDREN-儿童款")
    private String productType;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;
}
