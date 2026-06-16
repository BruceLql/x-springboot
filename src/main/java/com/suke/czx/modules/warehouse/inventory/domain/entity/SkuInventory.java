package com.suke.czx.modules.warehouse.inventory.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * SKU库存 - 领域实体
 */
@Data
@TableName("wms_sku_inventory")
@Schema(description = "SKU库存")
public class SkuInventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "库存ID")
    private String inventoryId;

    @Schema(description = "SKU ID")
    private String skuId;

    @Schema(description = "实际库存")
    private Integer realQuantity;

    @Schema(description = "锁定库存")
    private Integer withholdQuantity;

    @Schema(description = "占用库存")
    private Integer occupyQuantity;

    @Schema(description = "可售库存")
    private Integer sellableQuantity;

    @Version
    @Schema(description = "版本号(乐观锁)")
    private Integer version;

    @Schema(description = "租户ID")
    private Integer tenancyId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private Date updateTime;

    /**
     * 初始化SKU库存
     *
     * @param inventoryId 库存ID（由调用方通过ID生成器提供）
     */
    public SkuInventory initSkuInventory(String inventoryId, String skuId, Integer tenancyId) {
        SkuInventory skuInventory = new SkuInventory();
        skuInventory.setTenancyId(tenancyId);
        skuInventory.setInventoryId(inventoryId);
        skuInventory.setSkuId(skuId);
        skuInventory.setRealQuantity(0);
        skuInventory.setWithholdQuantity(0);
        skuInventory.setOccupyQuantity(0);
        skuInventory.setSellableQuantity(0);
        skuInventory.setCreateTime(new Date());
        skuInventory.setUpdateTime(new Date());
        return skuInventory;
    }
}
