package com.suke.czx.modules.warehouse.inventory.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 库存事件流水 - 领域实体
 */
@Data
@TableName("wms_inventory_event")
@Schema(description = "库存事件流水")
public class InventoryEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "事件ID")
    private String eventId;

    @Schema(description = "幂等键(确保请求唯一性)")
    private String idempotentKey;

    @Schema(description = "业务类型: INBOUND-入库, OUTBOUND-出库, SKU_COMBINATION-搭配, SKU_ARRANGEMENT-布款, STOCKTAKE-盘点")
    private String businessType;

    @Schema(description = "来源类型: GOODS-货物, SKU-规格")
    private String sourceType;

    @Schema(description = "来源ID")
    private String sourceId;

    @Schema(description = "事件类型: WITHHOLD-锁定, OCCUPY-占用, CONFIRM-确认, CANCEL-取消, INBOUND-入库, OUTBOUND-出库, ADJUST-调整")
    private String eventType;

    @Schema(description = "实际库存变化")
    private Integer changeReal;

    @Schema(description = "锁定库存变化")
    private Integer changeWithhold;

    @Schema(description = "占用库存变化")
    private Integer changeOccupy;

    @Schema(description = "可售库存变化")
    private Integer changeSellable;

    @Schema(description = "关联单号")
    private String relatedOrderNo;

    @Schema(description = "操作人ID")
    private String operatorId;

    @Schema(description = "租户ID")
    private Integer tenancyId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;

    public String assembleIdempotentKey() {
        if (StrUtil.isNotBlank(idempotentKey)) return idempotentKey;
        String key = businessType + "_" + sourceType + "_" + eventType + "_" + sourceId;
        if (StrUtil.isNotBlank(relatedOrderNo)) {
            key = key + "_" + relatedOrderNo;
        }
        return key;
    }
}
