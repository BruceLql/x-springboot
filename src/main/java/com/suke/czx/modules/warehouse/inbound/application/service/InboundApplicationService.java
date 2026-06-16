package com.suke.czx.modules.warehouse.inbound.application.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.common.utils.LeafSnowflakeGenerator;
import com.suke.czx.common.utils.UserUtil;
import com.suke.czx.modules.warehouse.goods.domain.entity.Goods;
import com.suke.czx.modules.warehouse.goods.infrastructure.repository.GoodsMapper;
import com.suke.czx.modules.warehouse.inbound.domain.entity.InboundItem;
import com.suke.czx.modules.warehouse.inbound.domain.entity.InboundOrder;
import com.suke.czx.modules.warehouse.inbound.infrastructure.repository.InboundItemMapper;
import com.suke.czx.modules.warehouse.inbound.infrastructure.repository.InboundOrderMapper;
import com.suke.czx.modules.warehouse.inventory.application.service.InventoryService;
import com.suke.czx.modules.warehouse.inventory.domain.command.CancelInboundDTO;
import com.suke.czx.modules.warehouse.inbound.interfaces.dto.command.InboundItemCommand;
import com.suke.czx.modules.warehouse.inventory.domain.command.IncreaseInventoryDTO;
import com.suke.czx.modules.warehouse.supplier.domain.entity.Supplier;
import com.suke.czx.modules.warehouse.supplier.domain.entity.SupplierGoodsRel;
import com.suke.czx.modules.warehouse.supplier.infrastructure.repository.SupplierGoodsRelMapper;
import com.suke.czx.modules.warehouse.supplier.infrastructure.repository.SupplierMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suke.czx.modules.warehouse.inbound.infrastructure.convert.InboundConvert;
import com.suke.czx.modules.warehouse.inbound.interfaces.vo.InboundItemVO;

import java.math.BigDecimal;
import java.util.*;
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
    private final InventoryService inventoryService;
    private final LeafSnowflakeGenerator leafSnowflakeGenerator;

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
    public List<InboundItemVO> getItems(String orderId) {
        List<InboundItem> items = inboundItemMapper.selectByOrderId(orderId);
        List<InboundItemVO> voList = InboundConvert.INSTANCE.toItemVOList(items);

        if (!items.isEmpty()) {
            Set<String> goodsIds = items.stream()
                    .map(InboundItem::getGoodsId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!goodsIds.isEmpty()) {
                List<Goods> goodsList = goodsMapper.selectBatchIds(goodsIds);
                Map<String, Goods> goodsMap = goodsList.stream()
                        .collect(Collectors.toMap(Goods::getGoodsId, g -> g));
                for (InboundItemVO vo : voList) {
                    Goods goods = goodsMap.get(vo.getGoodsId());
                    if (goods != null) {
                        vo.setGoodsName(goods.getGoodsName());
                        vo.setGoodsImage(goods.getGoodsImage());
                    }
                }
            }
        }

        return voList;
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
    public String batchInbound(String supplierId, List<InboundItemCommand> items, String remark) {
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
        
        // 4. 批量查询货物信息（避免N+1查询）
        List<String> goodsIdList = items.stream()
                .map(InboundItemCommand::getGoodsId)
                .collect(Collectors.toList());
        List<Goods> goodsList = goodsMapper.selectBatchIds(goodsIdList);
        Map<String, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getGoodsId, g -> g));

        // 5. 计算总数量和总成本
        int totalQuantity = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        
        for (InboundItemCommand item : items) {
            // 检查货物是否属于该供应商
            if (!supplierGoodsIds.contains(item.getGoodsId())) {
                throw new RRException("货物不属于该供应商: " + item.getGoodsId());
            }
            
            // 检查货物是否存在（从批量查询结果中获取）
            Goods goods = goodsMap.get(item.getGoodsId());
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
        order.setOrderId(leafSnowflakeGenerator.nextIdStr());
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
        for (InboundItemCommand itemDTO : items) {
            // 保存明细
            InboundItem item = new InboundItem();
            item.setItemId(leafSnowflakeGenerator.nextIdStr());
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
            IncreaseInventoryDTO increaseDTO = new IncreaseInventoryDTO();
            increaseDTO.setGoodsId(itemDTO.getGoodsId());
            increaseDTO.setQuantity(itemDTO.getQuantity());
            increaseDTO.setOrderNo(batchNo);
            increaseDTO.setOperatorId(operatorId);
            increaseDTO.setTenancyId(tenancyId);
            inventoryService.increaseInventory(increaseDTO);
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
        
        // 回滚库存：通过InventoryService取消入库
        String operatorId = UserUtil.getUserId();
        Integer tenancyId = UserUtil.getUserTenancyId();
        List<InboundItem> items = inboundItemMapper.selectByBatchNo(batchNo);
        for (InboundItem item : items) {
            CancelInboundDTO cancelDTO = new CancelInboundDTO();
            cancelDTO.setGoodsId(item.getGoodsId());
            cancelDTO.setQuantity(item.getQuantity());
            cancelDTO.setBatchNo(batchNo);
            cancelDTO.setOperatorId(operatorId);
            cancelDTO.setTenancyId(tenancyId);
            inventoryService.cancelInbound(cancelDTO);
            log.info("回滚库存成功: goodsId={}, quantity={}", item.getGoodsId(), item.getQuantity());
        }
        
        log.info("取消入库批次成功: batchNo={}", batchNo);
    }

    /**
     * 生成批次号
     */
    private String generateBatchNo() {
        return "IN" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + leafSnowflakeGenerator.nextId();
    }

}