package com.suke.czx.modules.warehouse.supplier.interfaces.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.suke.czx.common.annotation.SysLog;
import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.warehouse.supplier.application.service.SupplierApplicationService;
import com.suke.czx.modules.warehouse.supplier.interfaces.dto.command.SupplierBindGoodsCommand;
import com.suke.czx.modules.warehouse.supplier.interfaces.dto.command.SupplierCreateCommand;
import com.suke.czx.modules.warehouse.supplier.interfaces.dto.command.SupplierUnbindGoodsCommand;
import com.suke.czx.modules.warehouse.supplier.interfaces.dto.command.SupplierUpdateCommand;
import com.suke.czx.modules.warehouse.supplier.interfaces.dto.query.SupplierPageQuery;
import com.suke.czx.modules.warehouse.supplier.interfaces.vo.SupplierDetailVO;
import com.suke.czx.modules.warehouse.supplier.interfaces.vo.SupplierGoodsVO;
import com.suke.czx.modules.warehouse.supplier.interfaces.vo.SupplierVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商管理Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouse/supplier")
@Tag(name = "SupplierController", description = "供应商管理")
public class SupplierController extends AbstractController {

    private final SupplierApplicationService supplierApplicationService;

    /**
     * 分页查询供应商列表
     */
    @GetMapping("/list")
    @Operation(summary = "供应商列表", description = "分页查询供应商列表")
    public R list(@Valid SupplierPageQuery query) {
        IPage<SupplierVO> page = supplierApplicationService.queryPage(query);
        return R.ok().setData(page);
    }

    /**
     * 查询供应商详情
     */
    @GetMapping("/info/{supplierId}")
    @Operation(summary = "供应商详情", description = "根据ID查询供应商详情")
    public R info(@PathVariable String supplierId) {
        SupplierDetailVO vo = supplierApplicationService.getDetail(supplierId);
        return R.ok().setData(vo);
    }

    /**
     * 新增供应商
     */
    @SysLog("新增供应商")
    @PostMapping("/save")
    @Operation(summary = "新增供应商", description = "创建新的供应商信息")
    public R save(@Valid @RequestBody SupplierCreateCommand command) {
        supplierApplicationService.create(command);
        return R.ok("创建成功");
    }

    /**
     * 修改供应商
     */
    @SysLog("修改供应商")
    @PutMapping("/update")
    @Operation(summary = "修改供应商", description = "更新供应商信息")
    public R update(@Valid @RequestBody SupplierUpdateCommand command) {
        supplierApplicationService.update(command);
        return R.ok("更新成功");
    }

    /**
     * 删除供应商
     */
    @SysLog("删除供应商")
    @DeleteMapping("/delete/{supplierId}")
    @Operation(summary = "删除供应商", description = "删除指定供应商")
    public R delete(@PathVariable String supplierId) {
        supplierApplicationService.delete(supplierId);
        return R.ok("删除成功");
    }

    /**
     * 获取供应商下拉选项
     */
    @GetMapping("/options")
    @Operation(summary = "供应商选项", description = "获取供应商下拉列表")
    public R options() {
        List<SupplierVO> list = supplierApplicationService.getOptions();
        return R.ok().setData(list);
    }

    /**
     * 查询供应商关联的货物列表
     */
    @GetMapping("/goods/{supplierId}")
    @Operation(summary = "供应商货物", description = "查询供应商关联的货物列表")
    public R goods(@PathVariable String supplierId) {
        List<SupplierGoodsVO> list = supplierApplicationService.getSupplierGoods(supplierId);
        return R.ok().setData(list);
    }

    /**
     * 供应商绑定货物
     */
    @SysLog("供应商绑定货物")
    @PostMapping("/bindGoods")
    @Operation(summary = "绑定货物", description = "供应商绑定货物（支持批量）")
    public R bindGoods(@Valid @RequestBody SupplierBindGoodsCommand command) {
        supplierApplicationService.bindGoods(command);
        return R.ok("绑定成功");
    }

    /**
     * 供应商解绑货物
     */
    @SysLog("供应商解绑货物")
    @PostMapping("/unbindGoods")
    @Operation(summary = "解绑货物", description = "供应商解绑货物")
    public R unbindGoods(@Valid @RequestBody SupplierUnbindGoodsCommand command) {
        supplierApplicationService.unbindGoods(command);
        return R.ok("解绑成功");
    }
}
