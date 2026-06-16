package com.suke.czx.modules.warehouse.stocktake.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 盘点记录 - 领域实体
 */
@Data
@TableName("wms_stocktake_record")
@Schema(description = "盘点记录")
public class StocktakeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "盘点记录ID")
    private String recordId;

    @Schema(description = "库存类型: GOODS-货物, SKU-规格")
    private String stockType;

    @Schema(description = "来源ID(货物ID或SKU ID)")
    private String sourceId;

    @Schema(description = "盘点前数量")
    private Integer beforeQuantity;

    @Schema(description = "调整数量(正数增加,负数减少)")
    private Integer adjustQuantity;

    @Schema(description = "盘点后数量")
    private Integer afterQuantity;

    @Schema(description = "盘点原因")
    private String reason;

    @Schema(description = "是否已回滚: 0-否, 1-是")
    private Integer isRollback;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "回滚时间")
    private Date rollbackTime;

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
