package com.suke.czx.modules.warehouse.inventory.application.service;

import com.suke.czx.modules.warehouse.inventory.domain.entity.GoodsInventory;
import com.suke.czx.modules.warehouse.inventory.domain.command.*;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.BatchGoodsInventoryQueryDTO;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.GoodsInventoryPageQuery;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.GoodsInventoryQueryDTO;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.SkuInventoryPageQuery;
import com.suke.czx.modules.warehouse.inventory.interfaces.vo.GoodsInventoryVO;
import com.suke.czx.modules.warehouse.inventory.interfaces.vo.SkuInventoryVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 库存服务接口
 * 所有其他模块必须通过此接口调用库存功能，禁止直接调用库存领域服务或Mapper
 */
public interface InventoryService {

    /**
     * 分页查询货物库存列表
     */
    IPage<GoodsInventoryVO> queryPage(GoodsInventoryPageQuery query);

    /**
     * 分页查询SKU库存列表
     */
    IPage<SkuInventoryVO> querySkuPage(SkuInventoryPageQuery query);

    /**
     * 初始化货物库存
     */
    void initGoodsInventory(InitInventoryDTO dto);

    /**
     * 增加实际货物库存（入库）
     */
    void increaseInventory(IncreaseInventoryDTO dto);

    /**
     * 锁定库存（组商品）
     */
    void withholdInventory(WithholdInventoryDTO dto);

    /**
     * 释放锁定库存（取消组商品）
     */
    void releaseWithhold(ReleaseWithholdDTO dto);

    /**
     * 占用库存（布款完成）
     */
    void occupyInventory(OccupyInventoryDTO dto);

    /**
     * 占用库存（布款完成）批量操作
     */
    void occupyInventoryBatch(OccupyInventoryBatchDTO dto);

    /**
     * 取消占用库存（布款回滚）
     */
    void cancelOccupy(CancelOccupyDTO dto);

    /**
     * 取消sku库存（布款回滚）
     */
    void cancelSkuInventory(CancelOccupyDTO dto);

    /**
     * 确认出库（销售/组货流转）
     */
    void confirmOutbound(ConfirmOutboundDTO dto);

    /**
     * 报损出库
     */
    void damageOutbound(DamageOutboundDTO dto);

    /**
     * 取消入库（回滚入库库存）
     */
    void cancelInbound(CancelInboundDTO dto);

    /**
     * 盘点调整货物库存
     */
    void adjustGoodsInventory(AdjustInventoryDTO dto);

    /**
     * 初始化SKU库存
     */
    void initSkuInventory(String skuId, Integer tenancyId);

    /**
     * 查询有可售库存的货物库存列表
     */
    List<GoodsInventory> getSellableGoodsInventory();

    /**
     * 查询货物库存
     */
    GoodsInventory getGoodsInventory(GoodsInventoryQueryDTO dto);

    /**
     * 批量查询货物库存
     */
    List<GoodsInventory> batchGetGoodsInventory(BatchGoodsInventoryQueryDTO dto);


}
