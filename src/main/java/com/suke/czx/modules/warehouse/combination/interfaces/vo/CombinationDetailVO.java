package com.suke.czx.modules.warehouse.combination.interfaces.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * SKU搭配详情VO
 */
@Data
@Schema(description = "SKU搭配详情VO")
public class CombinationDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "SKU ID")
    private String skuId;

    @Schema(description = "SKU名称(搭配名称)")
    private String skuName;

    @Schema(description = "SKU图片")
    private String skuImage;

    @Schema(description = "预分配数量")
    private Integer preAllocateQuantity;

    @Schema(description = "状态: 0-已取消, 1-待布款, 2-已布款, 3-布款回滚")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "关联货物列表")
    private List<CombinationGoodsItemVO> goodsList;

    @Schema(description = "操作人ID")
    private String operatorId;

    @Schema(description = "租户ID")
    private Integer tenancyId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private Date updateTime;
}
