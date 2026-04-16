package com.suke.czx.modules.warehouse.combination.application.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.common.utils.UserUtil;
import com.suke.czx.modules.warehouse.combination.domain.entity.SkuDefinition;
import com.suke.czx.modules.warehouse.combination.domain.entity.SkuGoodsRel;
import com.suke.czx.modules.warehouse.combination.infrastructure.repository.SkuDefinitionMapper;
import com.suke.czx.modules.warehouse.combination.infrastructure.repository.SkuGoodsRelMapper;
import com.suke.czx.modules.warehouse.goods.domain.entity.Goods;
import com.suke.czx.modules.warehouse.goods.infrastructure.repository.GoodsMapper;
import com.suke.czx.modules.warehouse.inventory.domain.entity.GoodsInventory;
import com.suke.czx.modules.warehouse.inventory.domain.service.InventoryDomainService;
import com.suke.czx.modules.warehouse.inventory.infrastructure.repository.GoodsInventoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 组商品应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CombinationApplicationService {

    private final SkuDefinitionMapper skuDefinitionMapper;
    private final SkuGoodsRelMapper skuGoodsRelMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsInventoryMapper goodsInventoryMapper;
    private final InventoryDomainService inventoryDomainService;

    /**
     * 分页查询SKU列表
     */
    public IPage<SkuDefinition> queryPage(String skuName, Integer status, Long page, Long size) {
        LambdaQueryWrapper<SkuDefinition> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isNotBlank(skuName)) {
            wrapper.like(SkuDefinition::getSkuName, skuName);
        }
        if (status != null) {
            wrapper.eq(SkuDefinition::getStatus, status);
        }
        
        wrapper.orderByDesc(SkuDefinition::getCreateTime);
        
        Page<SkuDefinition> pageParam = new Page<>(page, size);
        return skuDefinitionMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 查询SKU详情
     */
    public SkuDefinition getDetail(String skuId) {
        return skuDefinitionMapper.selectById(skuId);
    }

    /**
     * 创建SKU搭配
     */
    @Transactional(rollbackFor = Exception.class)
    public String createSku(String skuName, String skuImage, Integer preAllocateQuantity, List<String> goodsIds) {
        if (goodsIds == null || goodsIds.isEmpty()) {
            throw new RRException("货物列表不能为空");
        }
        
        // 检查货物是否存在且有库存
        for (String goodsId : goodsIds) {
            Goods goods = goodsMapper.selectById(goodsId);
            if (goods == null) {
                throw new RRException("货物不存在: " + goodsId);
            }
            
            GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
            if (inventory == null || inventory.getSellableQuantity() < preAllocateQuantity) {
                throw new RRException("货物库存不足: " + goods.getGoodsName());
            }
        }
        
        String operatorId = UserUtil.getUserId();
        Integer tenancyId = UserUtil.getUserTenancyId();
        
        // 创建SKU
        SkuDefinition sku = new SkuDefinition();
        sku.setSkuId(cn.hutool.core.util.IdUtil.simpleUUID());
        sku.setSkuName(skuName);
        sku.setSkuImage(skuImage);
        sku.setPreAllocateQuantity(preAllocateQuantity);
        sku.setStatus(1); // 待布款
        sku.setGoodsIds(StrUtil.join(",", goodsIds));
        sku.setOperatorId(operatorId);
        sku.setTenancyId(tenancyId);
        sku.setCreateTime(new Date());
        sku.setUpdateTime(new Date());
        
        skuDefinitionMapper.insert(sku);
        
        // 创建货物关联
        for (String goodsId : goodsIds) {
            SkuGoodsRel rel = new SkuGoodsRel();
            rel.setSkuId(sku.getSkuId());
            rel.setGoodsId(goodsId);
            rel.setQuantity(1);
            rel.setCreateTime(new Date());
            rel.setUpdateTime(new Date());
            skuGoodsRelMapper.insert(rel);
            
            // 锁定库存
            inventoryDomainService.withholdInventory(goodsId, preAllocateQuantity, sku.getSkuId(), operatorId, tenancyId);
        }
        
        log.info("创建SKU搭配成功: skuId={}, skuName={}", sku.getSkuId(), skuName);
        return sku.getSkuId();
    }

    /**
     * 确定布款
     */
    @Transactional(rollbackFor = Exception.class)
    public void arrangeSku(String skuId) {
        SkuDefinition sku = skuDefinitionMapper.selectById(skuId);
        if (sku == null) {
            throw new RRException("SKU不存在");
        }
        if (sku.getStatus() != 1) {
            throw new RRException("只有待布款状态可以确认布款");
        }
        
        String operatorId = UserUtil.getUserId();
        Integer tenancyId = UserUtil.getUserTenancyId();
        
        // 查询关联货物
        List<SkuGoodsRel> relList = skuGoodsRelMapper.selectBySkuId(skuId);
        
        // 占用库存
        for (SkuGoodsRel rel : relList) {
            inventoryDomainService.occupyInventory(rel.getGoodsId(), sku.getPreAllocateQuantity(), skuId, operatorId, tenancyId);
        }
        
        // 更新SKU状态
        sku.setStatus(2); // 已布款
        sku.setUpdateTime(new Date());
        skuDefinitionMapper.updateById(sku);
        
        log.info("确定布款成功: skuId={}", skuId);
    }

    /**
     * 取消布款
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelSku(String skuId) {
        SkuDefinition sku = skuDefinitionMapper.selectById(skuId);
        if (sku == null) {
            throw new RRException("SKU不存在");
        }
        if (sku.getStatus() != 1) {
            throw new RRException("只有待布款状态可以取消");
        }
        
        String operatorId = UserUtil.getUserId();
        Integer tenancyId = UserUtil.getUserTenancyId();
        
        // 查询关联货物
        List<SkuGoodsRel> relList = skuGoodsRelMapper.selectBySkuId(skuId);
        
        // 释放锁定库存
        for (SkuGoodsRel rel : relList) {
            inventoryDomainService.releaseWithhold(rel.getGoodsId(), sku.getPreAllocateQuantity(), skuId, operatorId, tenancyId);
        }
        
        // 更新SKU状态
        sku.setStatus(0); // 已取消
        sku.setUpdateTime(new Date());
        skuDefinitionMapper.updateById(sku);
        
        log.info("取消布款成功: skuId={}", skuId);
    }

    /**
     * 布款回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackSku(String skuId) {
        SkuDefinition sku = skuDefinitionMapper.selectById(skuId);
        if (sku == null) {
            throw new RRException("SKU不存在");
        }
        if (sku.getStatus() != 2) {
            throw new RRException("只有已布款状态可以回滚");
        }
        
        String operatorId = UserUtil.getUserId();
        Integer tenancyId = UserUtil.getUserTenancyId();
        
        // 查询关联货物
        List<SkuGoodsRel> relList = skuGoodsRelMapper.selectBySkuId(skuId);
        
        // 取消占用库存
        for (SkuGoodsRel rel : relList) {
            inventoryDomainService.cancelOccupy(rel.getGoodsId(), sku.getPreAllocateQuantity(), skuId, operatorId, tenancyId);
        }
        
        // 更新SKU状态
        sku.setStatus(0); // 已取消
        sku.setUpdateTime(new Date());
        skuDefinitionMapper.updateById(sku);
        
        log.info("布款回滚成功: skuId={}", skuId);
    }

    /**
     * 删除SKU（仅待布款可删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSku(String skuId) {
        SkuDefinition sku = skuDefinitionMapper.selectById(skuId);
        if (sku == null) {
            throw new RRException("SKU不存在");
        }
        if (sku.getStatus() != 1) {
            throw new RRException("只有待布款状态可以删除");
        }
        
        String operatorId = UserUtil.getUserId();
        Integer tenancyId = UserUtil.getUserTenancyId();
        
        // 查询关联货物
        List<SkuGoodsRel> relList = skuGoodsRelMapper.selectBySkuId(skuId);
        
        // 释放锁定库存
        for (SkuGoodsRel rel : relList) {
            inventoryDomainService.releaseWithhold(rel.getGoodsId(), sku.getPreAllocateQuantity(), skuId, operatorId, tenancyId);
        }
        
        // 删除关联
        skuGoodsRelMapper.deleteBySkuId(skuId);
        
        // 删除SKU
        skuDefinitionMapper.deleteById(skuId);
        
        log.info("删除SKU成功: skuId={}", skuId);
    }

    /**
     * 查询可搭配的货物列表
     */
    public List<Goods> getAvailableGoods() {
        // 查询有库存的货物
        List<Goods> goodsList = goodsMapper.selectActiveGoods();
        return goodsList.stream()
                .filter(goods -> {
                    GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goods.getGoodsId());
                    return inventory != null && inventory.getSellableQuantity() > 0;
                })
                .collect(Collectors.toList());
    }
}
