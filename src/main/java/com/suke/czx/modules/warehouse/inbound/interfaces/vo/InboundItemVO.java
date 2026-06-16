package com.suke.czx.modules.warehouse.inbound.interfaces.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 入库明细VO
 */
@Data
@Schema(description = "入库明细VO")
public class InboundItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "明细ID")
    private String itemId;

    @Schema(description = "入库单ID")
    private String orderId;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "货物ID")
    private String goodsId;

    @Schema(description = "货物名称")
    private String goodsName;

    @Schema(description = "货物图片URL")
    private String goodsImage;

    @Schema(description = "入库数量")
    private Integer quantity;

    @Schema(description = "单个成本(元)")
    private BigDecimal unitCost;

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;
}
