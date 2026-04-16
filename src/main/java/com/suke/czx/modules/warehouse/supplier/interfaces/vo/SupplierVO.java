package com.suke.czx.modules.warehouse.supplier.interfaces.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 供应商信息VO
 */
@Data
@Schema(description = "供应商信息VO")
public class SupplierVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "供应商ID")
    private String supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "联系方式")
    private String contactPhone;

    @Schema(description = "类型")
    private String supplierType;

    @Schema(description = "类型描述")
    private String supplierTypeDesc;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;
}
