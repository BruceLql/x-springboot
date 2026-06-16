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
 * 货物库存 - 领域实体
 */
@Data
@TableName("wms_goods_inventory")
@Schema(description = "货物库存")
public class GoodsInventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "库存ID")
    private String inventoryId;

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "实际库存")
    private Integer realQuantity;

    @Schema(description = "锁定库存(搭配占用)")
    private Integer withholdQuantity;

    @Schema(description = "占用库存(布款完成)")
    private Integer occupyQuantity;

    @Schema(description = "可售库存 = real - withhold - occupy")
    private Integer sellableQuantity;

    /**
     * 版本号（乐观锁），两种工作模式：
     * <ul>
     *   <li>内置方法：MyBatis-Plus OptimisticLockerInnerInterceptor 自动追加 WHERE version=?</li>
     *   <li>自定义SQL：GoodsInventoryMapper.xml 中手动管理 version=version+1 和 WHERE version=#{version}</li>
     * </ul>
     * 注意：不要对库存操作使用 updateById() 等内置方法，始终通过 Mapper 的自定义 SQL 操作。
     */
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
     * 计算可售库存
     */
    public void calculateSellableQuantity() {
        this.sellableQuantity = (this.realQuantity != null ? this.realQuantity : 0)
                - (this.withholdQuantity != null ? this.withholdQuantity : 0)
                - (this.occupyQuantity != null ? this.occupyQuantity : 0);
    }
}
