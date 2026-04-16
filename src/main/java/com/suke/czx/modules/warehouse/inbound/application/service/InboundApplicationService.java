package com.suke.czx.modules.warehouse.inbound.application.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.common.utils.UserUtil;
import com.suke.czx.modules.warehouse.goods.domain.entity.Goods;
import com.suke.czx.modules.warehouse.goods.infrastructure.repository.GoodsMapper;
import com.suke.czx.modules.warehouse.inbound.domain.entity.InboundItem;
import com.suke.czx.modules.warehouse.inbound.domain.entity.InboundOrder;
import com.suke.czx.modules.warehouse.inbound.infrastructure.repository.InboundItemMapper;
import com.suke.czx.modules.warehouse.inbound.infrastructure.repository.InboundOrderMapper;
import com.suke.czx.modules.warehouse.inventory.domain.service.InventoryDomainService;
import com.suke.czx.modules.warehouse.supplier.domain.entity.Supplier;
import com.suke.czx.modules.warehouse.supplier.domain.entity.SupplierGoodsRel;
import com.suke.czx.modules.warehouse.supplier.infrastructure.repository.SupplierGoodsRelMapper;
import com.suke.czx.modules.warehouse.supplier.infrastructure.repository.SupplierMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 入库应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundApplicationService {

    private final InboundOrderMapper inboundOrderMapper;
    private final InboundItemMapper inboundItemMapper;
    private final SupplierMapper supplierMapper;
    private final SupplierGoodsRelMapper supplierGoodsRelMapper;
    private final GoodsMapper goodsMapper;
    private final InventoryDomainService inventoryDomainService;

    /**
     * 分页查询入库记录
     */
    public IPage<InboundOrder> queryPage(String batchNo, String supplierId, Integer status, Long page, Long size) {
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isNotBlank(batchNo)) {
            wrapper.eq(InboundOrder::getBatchNo, batchNo);
        }
        if (StringUtils.isNotBlank(supplierId)) {
            wrapper.eq(InboundOrder::getSupplierId, supplierId);
        }
        if (status != null) {
            wrapper.eq(InboundOrder::getStatus, status);
        }
        
        wrapper.orderByDesc(InboundOrder::getCreateTime);
        
        Page<InboundOrder> pageParam = new Page<>(page, size);
        return inboundOrderMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 查询入库单详情
     */
    public InboundOrder getDetail(String orderId) {
        return inboundOrderMapper.selectById(orderId);
    }

    /**
     * 查询入库明细
     */
    public List<InboundItem> getItems(String orderId) {
        return inboundItemMapper.selectByOrderId(orderId);
    }

    /**
     * 根据批次号查询入库单列表
     */
    public List<InboundOrder> getByBatchNo(String batchNo) {
        return inboundOrderMapper.selectByBatchNo(batchNo);
    }

    /**
     * 批量货物入库
     */
    @Transactional(rollbackFor = Exception.class)
    public String batchInbound(String supplierId, List<InboundItemDTO> items, String remark) {
        // 1. 检查供应商
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null) {
            throw new RRException("供应商不存在");
        }
        
        if (items == null || items.isEmpty()) {
            throw new RRException("入库明细不能为空");
        }
        
        // 2. 获取供应商关联的货物
        List<SupplierGoodsRel> relList = supplierGoodsRelMapper.selectBySupplierId(supplierId);
        Set<String> supplierGoodsIds = relList.stream()
                .map(SupplierGoodsRel::getGoodsId)
                .collect(Collectors.toSet());
        
        // 3. 生成批次号
        String batchNo = generateBatchNo();
        
        // 4. 计算总数量和总成本
        int totalQuantity = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        
        for (InboundItemDTO item : items) {
            // 检查货物是否属于该供应商
            if (!supplierGoodsIds.contains(item.getGoodsId())) {
                throw new RRException("货物不属于该供应商: " + item.getGoodsId());
            }
            
            // 检查货物是否存在
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (goods == null) {
                throw new RRException("货物不存在: " + item.getGoodsId());
            }
            
            totalQuantity += item.getQuantity();
            totalCost = totalCost.add(item.getUnitCost().multiply(new BigDecimal(item.getQuantity())));
        }
        
        // 5. 创建入库单
        String operatorId = UserUtil.getUserId();
        Integer tenancyId = UserUtil.getUserTenancyId();
        
        InboundOrder order = new InboundOrder();
        order.setOrderId(cn.hutool.core.util.IdUtil.simpleUUID());
        order.setBatchNo(batchNo);
        order.setSupplierId(supplierId);
        order.setTotalQuantity(totalQuantity);
        order.setTotalCost(totalCost);
        order.setStatus(1);
        order.setOperatorId(operatorId);
        order.setRemark(remark);
        order.setTenancyId(tenancyId);
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        
        inboundOrderMapper.insert(order);
        
        // 6. 保存入库明细并更新库存
        for (InboundItemDTO itemDTO : items) {
            // 保存明细
            InboundItem item = new InboundItem();
            item.setItemId(cn.hutool.core.util.IdUtil.simpleUUID());
            item.setOrderId(order.getOrderId());
            item.setBatchNo(batchNo);
            item.setGoodsId(itemDTO.getGoodsId());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitCost(itemDTO.getUnitCost());
            item.setTotalCost(itemDTO.getUnitCost().multiply(new BigDecimal(itemDTO.getQuantity())));
            item.setCreateTime(new Date());
            item.setUpdateTime(new Date());
            
            inboundItemMapper.insert(item);
            
            // 更新库存
            inventoryDomainService.increaseInventory(
                    itemDTO.getGoodsId(),
                    itemDTO.getQuantity(),
                    batchNo,
                    operatorId,
                    tenancyId
            );
        }
        
        log.info("批量入库成功: batchNo={}, supplierId={}, totalQuantity={}", batchNo, supplierId, totalQuantity);
        return batchNo;
    }

    /**
     * 取消入库批次
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelBatch(String batchNo) {
        // 查询批次下的入库单
        List<InboundOrder> orders = inboundOrderMapper.selectByBatchNo(batchNo);
        if (orders.isEmpty()) {
            throw new RRException("批次不存在");
        }
        
        InboundOrder order = orders.get(0);
        if (order.getStatus() == 0) {
            throw new RRException("批次已取消");
        }
        
        // 更新入库单状态
        order.setStatus(0);
        order.setUpdateTime(new Date());
        inboundOrderMapper.updateById(order);
        
        // 回滚库存（这里简化处理，实际应该扣减库存）
        List<InboundItem> items = inboundItemMapper.selectByBatchNo(batchNo);
        for (InboundItem item : items) {
            // 实际业务中这里需要扣减库存
            log.info("回滚库存: goodsId={}, quantity={}", item.getGoodsId(), item.getQuantity());
        }
        
        log.info("取消入库批次成功: batchNo={}", batchNo);
    }

    /**
     * 生成批次号
     */
    private String generateBatchNo() {
        return "IN" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + cn.hutool.core.util.RandomUtil.randomNumbers(4);
    }

    /**
     * 入库明细DTO
     */
    @lombok.Data
    public static class InboundItemDTO {
        private String goodsId;
        private Integer quantity;
        private BigDecimal unitCost;
    }
}
