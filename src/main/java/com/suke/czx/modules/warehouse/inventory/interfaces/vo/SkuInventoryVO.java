package com.suke.czx.modules.warehouse.inventory.interfaces.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * SKU库存VO
 */
@Data
@Schema(description = "SKU库存VO")
public class SkuInventoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "库存ID")
    private String inventoryId;

    @Schema(description = "SKU ID")
    private String skuId;

    @Schema(description = "SKU名称")
    private String skuName;

    @Schema(description = "SKU图片")
    private String skuImage;

    @Schema(description = "预分配数量")
    private Integer preAllocateQuantity;

    @Schema(description = "SKU状态: 0-已取消, 1-待布款, 2-已布款, 3-布款回滚")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "实际库存")
    private Integer realQuantity;

    @Schema(description = "锁定库存")
    private Integer withholdQuantity;

    @Schema(description = "占用库存")
    private Integer occupyQuantity;

    @Schema(description = "可售库存")
    private Integer sellableQuantity;

    @Schema(description = "版本号(乐观锁)")
    private Integer version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;
}
