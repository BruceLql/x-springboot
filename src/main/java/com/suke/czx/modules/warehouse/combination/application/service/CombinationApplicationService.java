package com.suke.czx.modules.warehouse.combination.application.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.common.utils.LeafSnowflakeGenerator;
import com.suke.czx.common.utils.UserUtil;
import com.suke.czx.modules.warehouse.combination.domain.entity.SkuDefinition;
import com.suke.czx.modules.warehouse.combination.domain.entity.SkuGoodsRel;
import com.suke.czx.modules.warehouse.combination.infrastructure.convert.CombinationConvert;
import com.suke.czx.modules.warehouse.combination.infrastructure.repository.SkuDefinitionMapper;
import com.suke.czx.modules.warehouse.combination.infrastructure.repository.SkuGoodsRelMapper;
import com.suke.czx.modules.warehouse.combination.interfaces.dto.command.CombinationCreateCommand;
import com.suke.czx.modules.warehouse.combination.interfaces.dto.query.CombinationPageQuery;
import com.suke.czx.modules.warehouse.combination.interfaces.vo.CombinationDetailVO;
import com.suke.czx.modules.warehouse.combination.interfaces.vo.CombinationVO;
import com.suke.czx.modules.warehouse.goods.domain.entity.Goods;
import com.suke.czx.modules.warehouse.goods.infrastructure.convert.GoodsConvert;
import com.suke.czx.modules.warehouse.goods.infrastructure.repository.GoodsMapper;
import com.suke.czx.modules.warehouse.goods.interfaces.vo.GoodsInventoryVO;
import com.suke.czx.modules.warehouse.inventory.application.service.InventoryService;
import com.suke.czx.modules.warehouse.inventory.domain.entity.GoodsInventory;
import com.suke.czx.modules.warehouse.inventory.domain.command.CancelOccupyDTO;
import com.suke.czx.modules.warehouse.inventory.domain.command.OccupyInventoryBatchDTO;
import com.suke.czx.modules.warehouse.inventory.domain.command.ReleaseWithholdDTO;
import com.suke.czx.modules.warehouse.inventory.domain.command.WithholdInventoryDTO;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.BatchGoodsInventoryQueryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    private final InventoryService inventoryService;
    private final CombinationConvert combinationConvert;
    private final GoodsConvert goodsConvert;
    private final LeafSnowflakeGenerator leafSnowflakeGenerator;

    /**
     * 分页查询SKU列表
     */
    public IPage<CombinationVO> queryPage(CombinationPageQuery query) {
        LambdaQueryWrapper<SkuDefinition> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(query.getSkuName())) {
            wrapper.like(SkuDefinition::getSkuName, query.getSkuName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SkuDefinition::getStatus, query.getStatus());
        }

        if (StringUtils.isNotBlank(query.getOrderBy())) {
            boolean isAsc = "asc".equalsIgnoreCase(query.getOrderDirection());
            wrapper.orderBy(true, isAsc, SkuDefinition::getCreateTime);
        } else {
            wrapper.orderByDesc(SkuDefinition::getCreateTime);
        }

        Page<SkuDefinition> pageParam = new Page<>(query.getPage(), query.getSize());
        IPage<SkuDefinition> skuPage = skuDefinitionMapper.selectPage(pageParam, wrapper);

        List<CombinationVO> voList = combinationConvert.toVOList(skuPage.getRecords());

        Page<CombinationVO> resultPage = new Page<>(skuPage.getCurrent(), skuPage.getSize(), skuPage.getTotal());
        resultPage.setRecords(voList);

        return resultPage;
    }

    /**
     * 查询SKU详情
     */
    public CombinationDetailVO getDetail(String skuId) {
        SkuDefinition sku = skuDefinitionMapper.selectById(skuId);
        if (sku == null) {
            throw new RRException("SKU不存在");
        }
        CombinationDetailVO vo = combinationConvert.toDetailVO(sku);

        // 填充关联货物列表
        if (StrUtil.isNotBlank(sku.getGoodsIds())) {
            List<String> goodsIdList = StrUtil.split(sku.getGoodsIds(), ",");
            List<Goods> goodsList = goodsMapper.selectBatchIds(goodsIdList);
            vo.setGoodsList(combinationConvert.convert(goodsList));
        }

        return vo;
    }

    /**
     * 创建SKU搭配
     */
    @Transactional(rollbackFor = Exception.class)
    public String createSku(CombinationCreateCommand command) {
        List<String> goodsIds = command.getGoodsIds();
        if (goodsIds == null || goodsIds.isEmpty()) {
            throw new RRException("货物列表不能为空");
        }

        Integer preAllocateQuantity = command.getPreAllocateQuantity();

        // 检查货物是否存在且有库存
        List<Goods> goodsList = goodsMapper.selectByIds(goodsIds);
        Map<String, Goods> goodsMap = goodsList.stream().collect(Collectors.toMap(Goods::getGoodsId, goods -> goods));
        for (String goodsId : goodsIds) {
            if (goodsMap.get(goodsId) == null) {
                throw new RRException("货物不存在: " + goodsId);
            }
        }
        // 通过InventoryService批量查询货物库存
        BatchGoodsInventoryQueryDTO batchQueryDTO = new BatchGoodsInventoryQueryDTO();
        batchQueryDTO.setGoodsIds(goodsIds);
        List<GoodsInventory> goodsInventories = inventoryService.batchGetGoodsInventory(batchQueryDTO);
        Map<String, GoodsInventory> goodsInventoryMap = goodsInventories.stream().collect(Collectors.toMap(GoodsInventory::getGoodsId, inventory -> inventory));
        for (String goodsId : goodsIds) {
            GoodsInventory inventory = goodsInventoryMap.get(goodsId);
            if (inventory == null || inventory.getSellableQuantity() < preAllocateQuantity) {
                throw new RRException("货物库存不足: " + goodsId);
            }
        }

        String operatorId = UserUtil.getUserId();
        Integer tenancyId = UserUtil.getUserTenancyId();

        // 创建SKU定义 搭配
        SkuDefinition sku = combinationConvert.toEntity(command);
        sku.setSkuId(leafSnowflakeGenerator.nextIdStr());
        sku.setStatus(1); // 待布款
        sku.setGoodsIds(StrUtil.join(",", goodsIds));
        sku.setOperatorId(operatorId);
        sku.setTenancyId(tenancyId);
        sku.setCreateTime(new Date());
        sku.setUpdateTime(new Date());

        skuDefinitionMapper.insert(sku);

        // 通过InventoryService初始化SKU库存
        inventoryService.initSkuInventory(sku.getSkuId(), tenancyId);

        // 创建货物关联（按goodsId排序获取锁，防止跨操作死锁）
        List<String> sortedGoodsIds = new ArrayList<>(goodsIds);
        Collections.sort(sortedGoodsIds);
        for (String goodsId : sortedGoodsIds) {
            SkuGoodsRel rel = new SkuGoodsRel();
            rel.setSkuId(sku.getSkuId());
            rel.setGoodsId(goodsId);
            rel.setQuantity(1);
            rel.setCreateTime(new Date());
            rel.setUpdateTime(new Date());
            skuGoodsRelMapper.insert(rel);

            // 锁定库存
            WithholdInventoryDTO withholdDTO = new WithholdInventoryDTO();
            withholdDTO.setGoodsId(goodsId);
            withholdDTO.setQuantity(preAllocateQuantity);
            withholdDTO.setSkuId(sku.getSkuId());
            withholdDTO.setOperatorId(operatorId);
            withholdDTO.setTenancyId(tenancyId);
            // 锁定货物库存
            inventoryService.withholdInventory(withholdDTO);
        }

        log.info("创建SKU搭配成功: skuId={}, skuName={}", sku.getSkuId(), sku.getSkuName());
        return sku.getSkuId();
    }

    /**
     * 确定布款
     */
    @Transactional(rollbackFor = Exception.class)
    public void arrangeSku(String skuId) {
        // 1.根据skuId查询SKU信息
        SkuDefinition sku = skuDefinitionMapper.selectById(skuId);
        if (sku == null) {
            throw new RRException("SKU不存在");
        }
        if (sku.getStatus() != 1) {
            throw new RRException("只有待布款状态可以确认布款");
        }

        String operatorId = UserUtil.getUserId();
        Integer tenancyId = UserUtil.getUserTenancyId();

        // 2.根据skuId查询关联货物
        List<SkuGoodsRel> relList = skuGoodsRelMapper.selectBySkuId(skuId);
        Set<String> goodsIdSet = relList.stream().map(SkuGoodsRel::getGoodsId).collect(Collectors.toSet());
        BatchGoodsInventoryQueryDTO batchQueryDTO = new BatchGoodsInventoryQueryDTO();
        batchQueryDTO.setGoodsIds(goodsIdSet.stream().toList());
        // 3.根据货物ID批量查询货物库存
        List<GoodsInventory> goodsInventories = inventoryService.batchGetGoodsInventory(batchQueryDTO);
        goodsInventories.forEach(inventory -> {
            Integer withholdQuantity = inventory.getWithholdQuantity();
            if (withholdQuantity != null && withholdQuantity < sku.getPreAllocateQuantity()) {
                throw new RRException("已锁定货物库存数量不足:" + sku.getPreAllocateQuantity() + "无法确认布款: " + inventory.getGoodsId());
            }
        });
        // 4.批量占用货物库存
        OccupyInventoryBatchDTO occupyInventoryBatch = new OccupyInventoryBatchDTO();
        occupyInventoryBatch.setGoodsIdList(goodsIdSet.stream().toList());
        occupyInventoryBatch.setSkuId(skuId);
        occupyInventoryBatch.setOperatorId(operatorId);
        occupyInventoryBatch.setTenancyId(tenancyId);
        occupyInventoryBatch.setQuantity(sku.getPreAllocateQuantity());
        inventoryService.occupyInventoryBatch(occupyInventoryBatch);

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

        // 查询关联货物（按goodsId排序获取锁，防止跨操作死锁）
        List<SkuGoodsRel> relList = skuGoodsRelMapper.selectBySkuId(skuId);
        relList.sort(Comparator.comparing(SkuGoodsRel::getGoodsId));

        // 释放锁定货物库存
        for (SkuGoodsRel rel : relList) {
            ReleaseWithholdDTO releaseDTO = new ReleaseWithholdDTO();
            releaseDTO.setGoodsId(rel.getGoodsId());
            releaseDTO.setQuantity(sku.getPreAllocateQuantity());
            releaseDTO.setSkuId(skuId);
            releaseDTO.setOperatorId(operatorId);
            releaseDTO.setTenancyId(tenancyId);
            inventoryService.releaseWithhold(releaseDTO);
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

        // 查询关联货物（按goodsId排序获取锁，防止跨操作死锁）
        List<SkuGoodsRel> relList = skuGoodsRelMapper.selectBySkuId(skuId);
        relList.sort(Comparator.comparing(SkuGoodsRel::getGoodsId));

        // 取消占用货物库存（每次创建新DTO避免复用问题）
        for (SkuGoodsRel rel : relList) {
            CancelOccupyDTO cancelDTO = new CancelOccupyDTO();
            cancelDTO.setGoodsId(rel.getGoodsId());
            cancelDTO.setQuantity(sku.getPreAllocateQuantity());
            cancelDTO.setSkuId(skuId);
            cancelDTO.setOperatorId(operatorId);
            cancelDTO.setTenancyId(tenancyId);
            inventoryService.cancelOccupy(cancelDTO);
        }

        // 取消sku库存
        CancelOccupyDTO skuCancelDTO = new CancelOccupyDTO();
        skuCancelDTO.setSkuId(skuId);
        skuCancelDTO.setQuantity(sku.getPreAllocateQuantity());
        skuCancelDTO.setOperatorId(operatorId);
        skuCancelDTO.setTenancyId(tenancyId);
        inventoryService.cancelSkuInventory(skuCancelDTO);

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

        // 查询关联货物（按goodsId排序获取锁，防止跨操作死锁）
        List<SkuGoodsRel> relList = skuGoodsRelMapper.selectBySkuId(skuId);
        relList.sort(Comparator.comparing(SkuGoodsRel::getGoodsId));

        // 释放锁定库存
        for (SkuGoodsRel rel : relList) {
            ReleaseWithholdDTO releaseDTO = new ReleaseWithholdDTO();
            releaseDTO.setGoodsId(rel.getGoodsId());
            releaseDTO.setQuantity(sku.getPreAllocateQuantity());
            releaseDTO.setSkuId(skuId);
            releaseDTO.setOperatorId(operatorId);
            releaseDTO.setTenancyId(tenancyId);
            inventoryService.releaseWithhold(releaseDTO);
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
    public List<GoodsInventoryVO> getAvailableGoods() {
        // 通过InventoryService查询有可售库存的货物
        List<GoodsInventory> goodsInventories = inventoryService.getSellableGoodsInventory();
        if (goodsInventories.isEmpty()) {
            return List.of();
        }
        Set<String> goodsIdSet = goodsInventories.stream().map(GoodsInventory::getGoodsId).collect(Collectors.toSet());
        List<Goods> goodsList = goodsMapper.selectByIds(goodsIdSet);
        return goodsConvert.toVOList(goodsList, goodsInventories);
    }
}