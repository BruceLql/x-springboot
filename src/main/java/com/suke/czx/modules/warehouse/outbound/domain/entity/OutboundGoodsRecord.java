package com.suke.czx.modules.warehouse.outbound.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 货物出库记录 - 领域实体
 */
@Data
@TableName("wms_outbound_goods_record")
@Schema(description = "货物出库记录")
public class OutboundGoodsRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "出库单ID")
    private String orderId;

    @Schema(description = "出库类型: DAMAGE-报损, COMBINATION-组货流转")
    private String orderType;

    @Schema(description = "来源类型: GOODS-货物")
    private String sourceType;

    @Schema(description = "货物ID")
    private String sourceId;

    @Schema(description = "出库数量")
    private Integer quantity;

    @Schema(description = "出库原因")
    private String reason;

    @Schema(description = "关联单号(如SKU订单号)")
    private String relatedOrderNo;

    @Schema(description = "状态: 0-取消, 1-已完成")
    private Integer status;

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
