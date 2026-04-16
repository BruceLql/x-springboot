package com.suke.czx.modules.warehouse.inventory.domain.service;

import cn.hutool.core.util.IdUtil;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.modules.warehouse.inventory.domain.entity.GoodsInventory;
import com.suke.czx.modules.warehouse.inventory.domain.entity.InventoryEvent;
import com.suke.czx.modules.warehouse.inventory.infrastructure.repository.GoodsInventoryMapper;
import com.suke.czx.modules.warehouse.inventory.infrastructure.repository.InventoryEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 库存领域服务
 * 提供核心的库存操作，确保数据一致性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryDomainService {

    private final GoodsInventoryMapper goodsInventoryMapper;
    private final InventoryEventMapper inventoryEventMapper;

    /**
     * 初始化货物库存
     */
    @Transactional(rollbackFor = Exception.class)
    public void initGoodsInventory(String goodsId, Integer tenancyId) {
        GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
        if (inventory != null) {
            return;
        }
        
        inventory = new GoodsInventory();
        inventory.setInventoryId(IdUtil.simpleUUID());
        inventory.setGoodsId(goodsId);
        inventory.setRealQuantity(0);
        inventory.setWithholdQuantity(0);
        inventory.setOccupyQuantity(0);
        inventory.setSellableQuantity(0);
        inventory.setVersion(0);
        inventory.setTenancyId(tenancyId);
        inventory.setCreateTime(new Date());
        inventory.setUpdateTime(new Date());
        
        goodsInventoryMapper.insert(inventory);
        log.info("初始化货物库存成功: goodsId={}", goodsId);
    }

    /**
     * 增加实际库存（入库）
     */
    @Transactional(rollbackFor = Exception.class)
    public void increaseInventory(String goodsId, int quantity, String orderNo, String operatorId, Integer tenancyId) {
        if (quantity <= 0) {
            throw new RRException("入库数量必须大于0");
        }
        
        // 查询库存
        GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
        if (inventory == null) {
            // 初始化库存
            initGoodsInventory(goodsId, tenancyId);
            inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
        }
        
        // 记录变化前
        int beforeReal = inventory.getRealQuantity();
        int beforeSellable = inventory.getSellableQuantity();
        
        // 更新库存（乐观锁）
        int rows = goodsInventoryMapper.increaseRealQuantity(goodsId, quantity, inventory.getVersion());
        if (rows == 0) {
            throw new RRException("库存更新失败，请重试");
        }
        
        // 记录库存事件
        recordInventoryEvent(
                "INBOUND", "GOODS", goodsId, "INBOUND",
                quantity, 0, 0, quantity,
                orderNo, operatorId, tenancyId
        );
        
        log.info("货物入库成功: goodsId={}, quantity={}, orderNo={}", goodsId, quantity, orderNo);
    }

    /**
     * 锁定库存（组商品）
     */
    @Transactional(rollbackFor = Exception.class)
    public void withholdInventory(String goodsId, int quantity, String skuId, String operatorId, Integer tenancyId) {
        if (quantity <= 0) {
            throw new RRException("锁定数量必须大于0");
        }
        
        GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
        if (inventory == null) {
            throw new RRException("库存不存在");
        }
        
        // 检查可售库存
        if (inventory.getSellableQuantity() < quantity) {
            throw new RRException("可售库存不足，无法锁定");
        }
        
        // 记录变化前
        int beforeWithhold = inventory.getWithholdQuantity();
        int beforeSellable = inventory.getSellableQuantity();
        
        // 更新库存
        int rows = goodsInventoryMapper.withholdQuantity(goodsId, quantity, inventory.getVersion());
        if (rows == 0) {
            throw new RRException("库存锁定失败，请重试");
        }
        
        // 记录库存事件
        recordInventoryEvent(
                "SKU_COMBINATION", "GOODS", goodsId, "WITHHOLD",
                0, quantity, 0, -quantity,
                skuId, operatorId, tenancyId
        );
        
        log.info("货物锁定库存成功: goodsId={}, quantity={}, skuId={}", goodsId, quantity, skuId);
    }

    /**
     * 释放锁定库存（取消组商品）
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseWithhold(String goodsId, int quantity, String skuId, String operatorId, Integer tenancyId) {
        if (quantity <= 0) {
            throw new RRException("释放数量必须大于0");
        }
        
        GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
        if (inventory == null) {
            throw new RRException("库存不存在");
        }
        
        if (inventory.getWithholdQuantity() < quantity) {
            throw new RRException("锁定库存不足，无法释放");
        }
        
        // 记录变化前
        int beforeWithhold = inventory.getWithholdQuantity();
        int beforeSellable = inventory.getSellableQuantity();
        
        // 更新库存
        int rows = goodsInventoryMapper.releaseWithhold(goodsId, quantity, inventory.getVersion());
        if (rows == 0) {
            throw new RRException("库存释放失败，请重试");
        }
        
        // 记录库存事件
        recordInventoryEvent(
                "SKU_COMBINATION", "GOODS", goodsId, "CANCEL",
                0, -quantity, 0, quantity,
                skuId, operatorId, tenancyId
        );
        
        log.info("货物释放锁定库存成功: goodsId={}, quantity={}, skuId={}", goodsId, quantity, skuId);
    }

    /**
     * 占用库存（布款完成）
     */
    @Transactional(rollbackFor = Exception.class)
    public void occupyInventory(String goodsId, int quantity, String skuId, String operatorId, Integer tenancyId) {
        if (quantity <= 0) {
            throw new RRException("占用数量必须大于0");
        }
        
        GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
        if (inventory == null) {
            throw new RRException("库存不存在");
        }
        
        if (inventory.getWithholdQuantity() < quantity) {
            throw new RRException("锁定库存不足，无法占用");
        }
        
        // 记录变化前
        int beforeWithhold = inventory.getWithholdQuantity();
        int beforeOccupy = inventory.getOccupyQuantity();
        
        // 更新库存
        int rows = goodsInventoryMapper.occupyQuantity(goodsId, quantity, inventory.getVersion());
        if (rows == 0) {
            throw new RRException("库存占用失败，请重试");
        }
        
        // 记录库存事件
        recordInventoryEvent(
                "SKU_ARRANGEMENT", "GOODS", goodsId, "OCCUPY",
                0, -quantity, quantity, 0,
                skuId, operatorId, tenancyId
        );
        
        log.info("货物占用库存成功: goodsId={}, quantity={}, skuId={}", goodsId, quantity, skuId);
    }

    /**
     * 取消占用库存（布款回滚）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOccupy(String goodsId, int quantity, String skuId, String operatorId, Integer tenancyId) {
        if (quantity <= 0) {
            throw new RRException("取消占用数量必须大于0");
        }
        
        GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
        if (inventory == null) {
            throw new RRException("库存不存在");
        }
        
        if (inventory.getOccupyQuantity() < quantity) {
            throw new RRException("占用库存不足，无法取消");
        }
        
        // 记录变化前
        int beforeReal = inventory.getRealQuantity();
        int beforeOccupy = inventory.getOccupyQuantity();
        int beforeSellable = inventory.getSellableQuantity();
        
        // 更新库存
        int rows = goodsInventoryMapper.cancelOccupy(goodsId, quantity, inventory.getVersion());
        if (rows == 0) {
            throw new RRException("取消占用失败，请重试");
        }
        
        // 记录库存事件
        recordInventoryEvent(
                "SKU_ARRANGEMENT", "GOODS", goodsId, "CANCEL",
                quantity, 0, -quantity, quantity,
                skuId, operatorId, tenancyId
        );
        
        log.info("货物取消占用库存成功: goodsId={}, quantity={}, skuId={}", goodsId, quantity, skuId);
    }

    /**
     * 确认出库（销售/组货流转）
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmOutbound(String goodsId, int quantity, String orderNo, String operatorId, Integer tenancyId) {
        if (quantity <= 0) {
            throw new RRException("出库数量必须大于0");
        }
        
        GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
        if (inventory == null) {
            throw new RRException("库存不存在");
        }
        
        if (inventory.getOccupyQuantity() < quantity || inventory.getRealQuantity() < quantity) {
            throw new RRException("库存不足，无法确认出库");
        }
        
        // 记录变化前
        int beforeReal = inventory.getRealQuantity();
        int beforeOccupy = inventory.getOccupyQuantity();
        
        // 更新库存
        int rows = goodsInventoryMapper.confirmOutbound(goodsId, quantity, inventory.getVersion());
        if (rows == 0) {
            throw new RRException("出库确认失败，请重试");
        }
        
        // 记录库存事件
        recordInventoryEvent(
                "OUTBOUND", "GOODS", goodsId, "CONFIRM",
                -quantity, 0, -quantity, 0,
                orderNo, operatorId, tenancyId
        );
        
        log.info("货物确认出库成功: goodsId={}, quantity={}, orderNo={}", goodsId, quantity, orderNo);
    }

    /**
     * 报损出库
     */
    @Transactional(rollbackFor = Exception.class)
    public void damageOutbound(String goodsId, int quantity, String orderNo, String operatorId, Integer tenancyId) {
        if (quantity <= 0) {
            throw new RRException("出库数量必须大于0");
        }
        
        GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
        if (inventory == null) {
            throw new RRException("库存不存在");
        }
        
        if (inventory.getRealQuantity() < quantity) {
            throw new RRException("实际库存不足，无法出库");
        }
        
        // 记录变化前
        int beforeReal = inventory.getRealQuantity();
        int beforeSellable = inventory.getSellableQuantity();
        
        // 更新库存
        int rows = goodsInventoryMapper.damageOutbound(goodsId, quantity, inventory.getVersion());
        if (rows == 0) {
            throw new RRException("报损出库失败，请重试");
        }
        
        // 记录库存事件
        recordInventoryEvent(
                "OUTBOUND", "GOODS", goodsId, "DAMAGE",
                -quantity, 0, 0, -quantity,
                orderNo, operatorId, tenancyId
        );
        
        log.info("货物报损出库成功: goodsId={}, quantity={}, orderNo={}", goodsId, quantity, orderNo);
    }

    /**
     * 查询货物库存
     */
    public GoodsInventory getGoodsInventory(String goodsId) {
        return goodsInventoryMapper.selectByGoodsId(goodsId);
    }

    /**
     * 记录库存事件
     */
    private void recordInventoryEvent(String businessType, String sourceType, String sourceId, String eventType,
                                      int changeReal, int changeWithhold, int changeOccupy, int changeSellable,
                                      String relatedOrderNo, String operatorId, Integer tenancyId) {
        InventoryEvent event = new InventoryEvent();
        event.setEventId(IdUtil.simpleUUID());
        event.setIdempotentKey(IdUtil.simpleUUID());
        event.setBusinessType(businessType);
        event.setSourceType(sourceType);
        event.setSourceId(sourceId);
        event.setEventType(eventType);
        event.setChangeReal(changeReal);
        event.setChangeWithhold(changeWithhold);
        event.setChangeOccupy(changeOccupy);
        event.setChangeSellable(changeSellable);
        event.setRelatedOrderNo(relatedOrderNo);
        event.setOperatorId(operatorId);
        event.setTenancyId(tenancyId);
        event.setCreateTime(new Date());
        
        inventoryEventMapper.insert(event);
    }
}
