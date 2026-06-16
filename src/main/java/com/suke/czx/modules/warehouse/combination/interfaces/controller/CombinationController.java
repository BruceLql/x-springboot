package com.suke.czx.modules.warehouse.combination.interfaces.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.suke.czx.common.annotation.SysLog;
import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.warehouse.combination.application.service.CombinationApplicationService;
import com.suke.czx.modules.warehouse.combination.interfaces.dto.command.CombinationCreateCommand;
import com.suke.czx.modules.warehouse.combination.interfaces.dto.query.CombinationPageQuery;
import com.suke.czx.modules.warehouse.combination.interfaces.vo.CombinationDetailVO;
import com.suke.czx.modules.warehouse.combination.interfaces.vo.CombinationVO;
import com.suke.czx.modules.warehouse.goods.interfaces.vo.GoodsInventoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组商品管理Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouse/combination")
@Tag(name = "CombinationController", description = "组商品管理")
public class CombinationController extends AbstractController {

    private final CombinationApplicationService combinationApplicationService;

    /**
     * 分页查询SKU列表
     */
    @GetMapping("/list")
    @Operation(summary = "SKU列表", description = "分页查询SKU搭配列表")
    public R list(@Valid CombinationPageQuery query) {
        IPage<CombinationVO> result = combinationApplicationService.queryPage(query);
        return R.ok().setData(result);
    }

    /**
     * 查询SKU详情
     */
    @GetMapping("/info/{skuId}")
    @Operation(summary = "SKU详情", description = "根据ID查询SKU详情")
    public R info(@PathVariable String skuId) {
        CombinationDetailVO vo = combinationApplicationService.getDetail(skuId);
        return R.ok().setData(vo);
    }

    /**
     * 创建SKU搭配
     */
    @SysLog("创建SKU搭配")
    @PostMapping("/create")
    @Operation(summary = "创建搭配", description = "创建新的SKU搭配")
    public R create(@Valid @RequestBody CombinationCreateCommand command) {
        String skuId = combinationApplicationService.createSku(command);
        return R.ok("创建成功").setData(skuId);
    }

    /**
     * 确定布款
     */
    @SysLog("确定布款")
    @PostMapping("/arrange/{skuId}")
    @Operation(summary = "确定布款", description = "确认SKU布款")
    public R arrange(@PathVariable String skuId) {
        combinationApplicationService.arrangeSku(skuId);
        return R.ok("布款成功");
    }

    /**
     * 取消布款
     */
    @SysLog("取消布款")
    @PostMapping("/cancel/{skuId}")
    @Operation(summary = "取消布款", description = "取消SKU布款")
    public R cancel(@PathVariable String skuId) {
        combinationApplicationService.cancelSku(skuId);
        return R.ok("取消成功");
    }

    /**
     * 布款回滚
     */
    @SysLog("布款回滚")
    @PostMapping("/rollback/{skuId}")
    @Operation(summary = "布款回滚", description = "回滚SKU布款")
    public R rollback(@PathVariable String skuId) {
        combinationApplicationService.rollbackSku(skuId);
        return R.ok("回滚成功");
    }

    /**
     * 删除SKU
     */
    @SysLog("删除SKU")
    @DeleteMapping("/delete/{skuId}")
    @Operation(summary = "删除SKU", description = "删除SKU搭配（仅待布款可删除）")
    public R delete(@PathVariable String skuId) {
        combinationApplicationService.deleteSku(skuId);
        return R.ok("删除成功");
    }

    /**
     * 查询可搭配的货物
     */
    @GetMapping("/availableGoods")
    @Operation(summary = "可搭配货物", description = "查询可搭配的货物列表")
    public R availableGoods() {
        List<GoodsInventoryVO> list = combinationApplicationService.getAvailableGoods();
        return R.ok().setData(list);
    }

}
