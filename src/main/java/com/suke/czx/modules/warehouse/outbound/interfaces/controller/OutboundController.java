package com.suke.czx.modules.warehouse.outbound.interfaces.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.suke.czx.common.annotation.SysLog;
import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.warehouse.outbound.application.service.OutboundApplicationService;
import com.suke.czx.modules.warehouse.outbound.domain.entity.OutboundGoodsRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 出库管理Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouse/outbound")
@Tag(name = "OutboundController", description = "出库管理")
public class OutboundController extends AbstractController {

    private final OutboundApplicationService outboundApplicationService;

    /**
     * 分页查询出库记录
     */
    @GetMapping("/list")
    @Operation(summary = "出库列表", description = "分页查询出库记录列表")
    public R list(@RequestParam(required = false) String orderType,
                  @RequestParam(required = false) String sourceId,
                  @RequestParam(defaultValue = "1") Long page,
                  @RequestParam(defaultValue = "10") Long size) {
        IPage<OutboundGoodsRecord> result = outboundApplicationService.queryPage(orderType, sourceId, page, size);
        return R.ok().setData(result);
    }

    /**
     * 报损出库
     */
    @SysLog("报损出库")
    @PostMapping("/damage")
    @Operation(summary = "报损出库", description = "货物报损出库")
    public R damage(@RequestBody DamageOutboundRequest request) {
        String orderNo = outboundApplicationService.damageOutbound(
                request.getGoodsId(),
                request.getQuantity(),
                request.getReason()
        );
        return R.ok("出库成功").setData(orderNo);
    }

    /**
     * 报损出库请求
     */
    @lombok.Data
    public static class DamageOutboundRequest {
        private String goodsId;
        private Integer quantity;
        private String reason;
    }
}
