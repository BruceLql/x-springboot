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
 * 入库明细表 - 领域实体
 */
@Data
@TableName("wms_inbound_item")
@Schema(description = "入库记录明细")
public class InboundItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "明细ID")
    private String itemId;

    @Schema(description = "入库单ID")
    private String orderId;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "入库数量")
    private Integer quantity;

    @Schema(description = "单个成本(元)")
    private BigDecimal unitCost;

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private Date updateTime;
}
