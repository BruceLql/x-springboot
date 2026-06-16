package com.suke.czx.modules.warehouse.stocktake.interfaces.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.suke.czx.common.annotation.SysLog;
import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.warehouse.stocktake.application.service.StocktakeApplicationService;
import com.suke.czx.modules.warehouse.stocktake.domain.entity.StocktakeRecord;
import com.suke.czx.modules.warehouse.stocktake.interfaces.dto.command.AdjustGoodsCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 盘点管理Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouse/stocktake")
@Tag(name = "StocktakeController", description = "盘点管理")
public class StocktakeController extends AbstractController {

    private final StocktakeApplicationService stocktakeApplicationService;

    /**
     * 分页查询盘点记录
     */
    @GetMapping("/list")
    @Operation(summary = "盘点记录列表", description = "分页查询盘点记录列表")
    public R list(@RequestParam(required = false) String stockType,
                  @RequestParam(defaultValue = "1") Long page,
                  @RequestParam(defaultValue = "10") Long size) {
        IPage<StocktakeRecord> result = stocktakeApplicationService.queryPage(stockType, page, size);
        return R.ok().setData(result);
    }

    /**
     * 货物库存盘点调整
     */
    @SysLog("货物库存盘点")
    @PostMapping("/adjustGoods")
    @Operation(summary = "货物盘点调整", description = "调整货物库存数量")
    public R adjustGoods(@RequestBody AdjustGoodsCommand command) {
        String recordId = stocktakeApplicationService.adjustGoodsInventory(
                command.getGoodsId(),
                command.getAdjustQuantity(),
                command.getReason()
        );
        return R.ok("盘点成功").setData(recordId);
    }

    /**
     * 盘点回滚
     */
    @SysLog("盘点回滚")
    @PostMapping("/rollback/{recordId}")
    @Operation(summary = "盘点回滚", description = "回滚盘点记录（仅一次）")
    public R rollback(@PathVariable String recordId) {
        stocktakeApplicationService.rollback(recordId);
        return R.ok("回滚成功");
    }
}
