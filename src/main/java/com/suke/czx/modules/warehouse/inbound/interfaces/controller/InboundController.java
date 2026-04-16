package com.suke.czx.modules.warehouse.inbound.interfaces.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.suke.czx.common.annotation.SysLog;
import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.warehouse.inbound.application.service.InboundApplicationService;
import com.suke.czx.modules.warehouse.inbound.domain.entity.InboundItem;
import com.suke.czx.modules.warehouse.inbound.domain.entity.InboundOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 入库管理Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouse/inbound")
@Tag(name = "InboundController", description = "入库管理")
public class InboundController extends AbstractController {

    private final InboundApplicationService inboundApplicationService;

    /**
     * 分页查询入库记录
     */
    @GetMapping("/list")
    @Operation(summary = "入库列表", description = "分页查询入库记录列表")
    public R list(@RequestParam(required = false) String batchNo,
                  @RequestParam(required = false) String supplierId,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(defaultValue = "1") Long page,
                  @RequestParam(defaultValue = "10") Long size) {
        IPage<InboundOrder> result = inboundApplicationService.queryPage(batchNo, supplierId, status, page, size);
        return R.ok().setData(result);
    }

    /**
     * 查询入库单详情
     */
    @GetMapping("/detail/{orderId}")
    @Operation(summary = "入库详情", description = "根据ID查询入库单详情")
    public R detail(@PathVariable String orderId) {
        InboundOrder order = inboundApplicationService.getDetail(orderId);
        return R.ok().setData(order);
    }

    /**
     * 查询入库明细
     */
    @GetMapping("/items/{orderId}")
    @Operation(summary = "入库明细", description = "查询入库单明细列表")
    public R items(@PathVariable String orderId) {
        List<InboundItem> items = inboundApplicationService.getItems(orderId);
        return R.ok().setData(items);
    }

    /**
     * 批量货物入库
     */
    @SysLog("批量货物入库")
    @PostMapping("/batch")
    @Operation(summary = "批量入库", description = "批量货物入库")
    public R batchInbound(@RequestBody BatchInboundRequest request) {
        String batchNo = inboundApplicationService.batchInbound(
                request.getSupplierId(),
                request.getItems(),
                request.getRemark()
        );
        return R.ok("入库成功").setData(batchNo);
    }

    /**
     * 取消入库批次
     */
    @SysLog("取消入库批次")
    @PostMapping("/cancel/{batchNo}")
    @Operation(summary = "取消入库", description = "取消入库批次")
    public R cancelBatch(@PathVariable String batchNo) {
        inboundApplicationService.cancelBatch(batchNo);
        return R.ok("取消成功");
    }

    /**
     * 批量入库请求
     */
    @lombok.Data
    public static class BatchInboundRequest {
        private String supplierId;
        private List<InboundApplicationService.InboundItemDTO> items;
        private String remark;
    }
}
