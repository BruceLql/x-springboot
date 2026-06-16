package com.suke.czx.modules.warehouse.inventory.domain.command;

import lombok.Builder;
import lombok.Data;

/**
 * 库存事件记录参数
 * 用于封装recordInventoryEvent方法的多个参数
 */
@Data
@Builder
public class InventoryEventParam {

    /** 业务类型: INBOUND-入库, OUTBOUND-出库, SKU_COMBINATION-搭配, SKU_ARRANGEMENT-布款, STOCKTAKE-盘点 */
    private String businessType;

    /** 来源类型: GOODS-货物, SKU-规格 */
    private String sourceType;

    /** 来源ID */
    private String sourceId;

    /** 事件类型: WITHHOLD-锁定, OCCUPY-占用, CONFIRM-确认, CANCEL-取消, INBOUND-入库, OUTBOUND-出库, ADJUST-调整 */
    private String eventType;

    /** 实际库存变化 */
    private int changeReal;

    /** 锁定库存变化 */
    private int changeWithhold;

    /** 占用库存变化 */
    private int changeOccupy;

    /** 可售库存变化 */
    private int changeSellable;

    /** 关联单号 */
    private String relatedOrderNo;

    /** 操作人ID */
    private String operatorId;

    /** 租户ID */
    private Integer tenancyId;
}
