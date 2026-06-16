package com.suke.czx.modules.warehouse.inventory.interfaces.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.warehouse.inventory.application.service.InventoryApplicationService;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.GoodsInventoryPageQuery;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.SkuInventoryPageQuery;
import com.suke.czx.modules.warehouse.inventory.interfaces.vo.GoodsInventoryVO;
import com.suke.czx.modules.warehouse.inventory.interfaces.vo.SkuInventoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存管理Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouse/inventory")
@Tag(name = "InventoryController", description = "库存管理")
public class InventoryController extends AbstractController {

    private final InventoryApplicationService inventoryApplicationService;

    /**
     * 分页查询货物库存列表
     */
    @GetMapping("/goods/list")
    @Operation(summary = "货物库存列表", description = "分页查询货物库存列表（支持按货物名称、类目、分类、状态筛选）")
    public R goodsList(GoodsInventoryPageQuery query) {
        IPage<GoodsInventoryVO> result = inventoryApplicationService.queryPage(query);
        return R.ok().setData(result);
    }

    /**
     * 分页查询SKU库存列表
     */
    @GetMapping("/sku/list")
    @Operation(summary = "SKU库存列表", description = "分页查询SKU库存列表（支持按SKU名称、状态筛选）")
    public R skuList(SkuInventoryPageQuery query) {
        IPage<SkuInventoryVO> result = inventoryApplicationService.querySkuPage(query);
        return R.ok().setData(result);
    }
}
