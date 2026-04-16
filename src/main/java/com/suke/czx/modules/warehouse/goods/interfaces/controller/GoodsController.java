package com.suke.czx.modules.warehouse.goods.interfaces.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.suke.czx.common.annotation.SysLog;
import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.warehouse.goods.application.service.GoodsApplicationService;
import com.suke.czx.modules.warehouse.goods.interfaces.dto.command.GoodsCreateCommand;
import com.suke.czx.modules.warehouse.goods.interfaces.dto.command.GoodsUpdateCommand;
import com.suke.czx.modules.warehouse.goods.interfaces.dto.query.GoodsPageQuery;
import com.suke.czx.modules.warehouse.goods.interfaces.vo.GoodsDetailVO;
import com.suke.czx.modules.warehouse.goods.interfaces.vo.GoodsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 货物管理Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouse/goods")
@Tag(name = "GoodsController", description = "货物管理")
public class GoodsController extends AbstractController {

    private final GoodsApplicationService goodsApplicationService;

    /**
     * 分页查询货物列表
     */
    @GetMapping("/list")
    @Operation(summary = "货物列表", description = "分页查询货物列表")
    public R list(@Valid GoodsPageQuery query) {
        IPage<GoodsVO> page = goodsApplicationService.queryPage(query);
        return R.ok().setData(page);
    }

    /**
     * 查询货物详情
     */
    @GetMapping("/info/{goodsId}")
    @Operation(summary = "货物详情", description = "根据ID查询货物详情")
    public R info(@PathVariable String goodsId) {
        GoodsDetailVO vo = goodsApplicationService.getDetail(goodsId);
        return R.ok().setData(vo);
    }

    /**
     * 新增货物
     */
    @SysLog("新增货物")
    @PostMapping("/save")
    @Operation(summary = "新增货物", description = "创建新的货物信息")
    public R save(@Valid @RequestBody GoodsCreateCommand command) {
        goodsApplicationService.create(command);
        return R.ok("创建成功");
    }

    /**
     * 修改货物
     */
    @SysLog("修改货物")
    @PutMapping("/update")
    @Operation(summary = "修改货物", description = "更新货物信息")
    public R update(@Valid @RequestBody GoodsUpdateCommand command) {
        goodsApplicationService.update(command);
        return R.ok("更新成功");
    }

    /**
     * 删除货物
     */
    @SysLog("删除货物")
    @DeleteMapping("/delete/{goodsId}")
    @Operation(summary = "删除货物", description = "删除指定货物")
    public R delete(@PathVariable String goodsId) {
        goodsApplicationService.delete(goodsId);
        return R.ok("删除成功");
    }

    /**
     * 获取货物下拉选项
     */
    @GetMapping("/options")
    @Operation(summary = "货物选项", description = "获取货物下拉列表（用于入库选择）")
    public R options() {
        List<GoodsVO> list = goodsApplicationService.getOptions();
        return R.ok().setData(list);
    }
}
