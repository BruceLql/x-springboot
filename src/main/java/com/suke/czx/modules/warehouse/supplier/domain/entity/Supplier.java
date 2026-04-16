package com.suke.czx.modules.warehouse.supplier.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 供应商信息 - 领域实体
 */
@Data
@TableName("wms_supplier")
@Schema(description = "供应商信息")
public class Supplier implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "供应商ID")
    private String supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "联系方式")
    private String contactPhone;

    @Schema(description = "类型: FACTORY-源头厂家, DEALER-二道贩子")
    private String supplierType;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    @Schema(description = "租户ID")
    private Integer tenancyId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private Date updateTime;
}
