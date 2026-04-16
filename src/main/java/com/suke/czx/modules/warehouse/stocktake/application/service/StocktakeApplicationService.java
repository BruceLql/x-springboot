package com.suke.czx.modules.warehouse.stocktake.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.common.utils.UserUtil;
import com.suke.czx.modules.warehouse.inventory.domain.entity.GoodsInventory;
import com.suke.czx.modules.warehouse.inventory.infrastructure.repository.GoodsInventoryMapper;
import com.suke.czx.modules.warehouse.stocktake.domain.entity.StocktakeRecord;
import com.suke.czx.modules.warehouse.stocktake.infrastructure.repository.StocktakeRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 盘点应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StocktakeApplicationService {

    private final StocktakeRecordMapper stocktakeRecordMapper;
    private final GoodsInventoryMapper goodsInventoryMapper;

    /**
     * 分页查询盘点记录
     */
    public IPage<StocktakeRecord> queryPage(String stockType, Long page, Long size) {
        LambdaQueryWrapper<StocktakeRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (stockType != null) {
            wrapper.eq(StocktakeRecord::getStockType, stockType);
        }
        
        wrapper.orderByDesc(StocktakeRecord::getCreateTime);
        
        Page<StocktakeRecord> pageParam = new Page<>(page, size);
        return stocktakeRecordMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 货物库存盘点调整
     */
    @Transactional(rollbackFor = Exception.class)
    public String adjustGoodsInventory(String goodsId, Integer adjustQuantity, String reason) {
        String operatorId = UserUtil.getUserId();
        Integer tenancyId = UserUtil.getUserTenancyId();
        
        // 查询当前库存
        GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
        if (inventory == null) {
            throw new RRException("库存不存在");
        }
        
        int beforeQuantity = inventory.getRealQuantity();
        int afterQuantity = beforeQuantity + adjustQuantity;
        
        if (afterQuantity < 0) {
            throw new RRException("调整后库存不能为负数");
        }
        
        // 更新库存
        inventory.setRealQuantity(afterQuantity);
        inventory.calculateSellableQuantity();
        inventory.setUpdateTime(new Date());
        goodsInventoryMapper.updateById(inventory);
        
        // 保存盘点记录
        StocktakeRecord record = new StocktakeRecord();
        record.setRecordId(cn.hutool.core.util.IdUtil.simpleUUID());
        record.setStockType("GOODS");
        record.setSourceId(goodsId);
        record.setBeforeQuantity(beforeQuantity);
        record.setAdjustQuantity(adjustQuantity);
        record.setAfterQuantity(afterQuantity);
        record.setReason(reason);
        record.setIsRollback(0);
        record.setOperatorId(operatorId);
        record.setTenancyId(tenancyId);
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        
        stocktakeRecordMapper.insert(record);
        
        log.info("货物库存盘点调整成功: goodsId={}, adjustQuantity={}, recordId={}", 
                goodsId, adjustQuantity, record.getRecordId());
        return record.getRecordId();
    }

    /**
     * 盘点回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollback(String recordId) {
        StocktakeRecord record = stocktakeRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RRException("盘点记录不存在");
        }
        if (record.getIsRollback() == 1) {
            throw new RRException("盘点记录已回滚");
        }
        
        // 回滚库存
        if ("GOODS".equals(record.getStockType())) {
            GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(record.getSourceId());
            if (inventory != null) {
                // 反向调整
                int newQuantity = inventory.getRealQuantity() - record.getAdjustQuantity();
                if (newQuantity < 0) {
                    throw new RRException("回滚后库存不能为负数");
                }
                inventory.setRealQuantity(newQuantity);
                inventory.calculateSellableQuantity();
                inventory.setUpdateTime(new Date());
                goodsInventoryMapper.updateById(inventory);
            }
        }
        
        // 更新盘点记录
        record.setIsRollback(1);
        record.setRollbackTime(new Date());
        record.setUpdateTime(new Date());
        stocktakeRecordMapper.updateById(record);
        
        log.info("盘点回滚成功: recordId={}", recordId);
    }
}
