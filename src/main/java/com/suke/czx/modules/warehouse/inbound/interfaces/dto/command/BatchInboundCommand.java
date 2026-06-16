package com.suke.czx.modules.warehouse.inbound.interfaces.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量入库命令
 */
@Data
@Schema(description = "批量入库命令")
public class BatchInboundCommand {

    @Schema(description = "供应商ID")
    private String supplierId;

    @Schema(description = "入库明细列表")
    private List<InboundItemCommand> items;

    @Schema(description = "备注")
    private String remark;
}
