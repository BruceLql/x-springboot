package com.suke.czx.modules.warehouse.inbound.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 入库单主表 - 领域实体
 */
@Data
@TableName("wms_inbound_order")
@Schema(description = "入库记录主表")
public class InboundOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "入库单ID")
    private String orderId;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "供应商ID")
    private String supplierId;

    @Schema(description = "总数量")
    private Integer totalQuantity;

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @Schema(description = "状态: 0-取消, 1-已完成")
    private Integer status;

    @Schema(description = "操作人ID")
    private String operatorId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "租户ID")
    private Integer tenancyId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private Date updateTime;
}
