package com.suke.czx.modules.warehouse.supplier.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.modules.warehouse.goods.domain.entity.Goods;
import com.suke.czx.modules.warehouse.goods.infrastructure.repository.GoodsMapper;
import com.suke.czx.modules.warehouse.supplier.domain.entity.Supplier;
import com.suke.czx.modules.warehouse.supplier.domain.entity.SupplierGoodsRel;
import com.suke.czx.modules.warehouse.supplier.infrastructure.convert.SupplierConvert;
import com.suke.czx.modules.warehouse.supplier.infrastructure.repository.SupplierGoodsRelMapper;
import com.suke.czx.modules.warehouse.supplier.infrastructure.repository.SupplierMapper;
import com.suke.czx.modules.warehouse.supplier.interfaces.dto.command.SupplierBindGoodsCommand;
import com.suke.czx.modules.warehouse.supplier.interfaces.dto.command.SupplierCreateCommand;
import com.suke.czx.modules.warehouse.supplier.interfaces.dto.command.SupplierUnbindGoodsCommand;
import com.suke.czx.modules.warehouse.supplier.interfaces.dto.command.SupplierUpdateCommand;
import com.suke.czx.modules.warehouse.supplier.interfaces.dto.query.SupplierPageQuery;
import com.suke.czx.modules.warehouse.supplier.interfaces.vo.SupplierDetailVO;
import com.suke.czx.modules.warehouse.supplier.interfaces.vo.SupplierGoodsVO;
import com.suke.czx.modules.warehouse.supplier.interfaces.vo.SupplierVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 供应商应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierApplicationService {

    private final SupplierMapper supplierMapper;
    private final SupplierGoodsRelMapper supplierGoodsRelMapper;
    private final GoodsMapper goodsMapper;
    private final SupplierConvert supplierConvert;

    /**
     * 分页查询供应商列表
     */
    public IPage<SupplierVO> queryPage(SupplierPageQuery query) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        
        // 模糊查询供应商名称
        if (StringUtils.isNotBlank(query.getSupplierName())) {
            wrapper.like(Supplier::getSupplierName, query.getSupplierName());
        }
        
        // 类型筛选
        if (StringUtils.isNotBlank(query.getSupplierType())) {
            wrapper.eq(Supplier::getSupplierType, query.getSupplierType());
        }
        
        // 状态筛选
        if (query.getStatus() != null) {
            wrapper.eq(Supplier::getStatus, query.getStatus());
        }
        
        // 排序
        if (StringUtils.isNotBlank(query.getOrderBy())) {
            boolean isAsc = "asc".equalsIgnoreCase(query.getOrderDirection());
            wrapper.orderBy(true, isAsc, Supplier::getCreateTime);
        } else {
            wrapper.orderByDesc(Supplier::getCreateTime);
        }
        
        Page<Supplier> page = new Page<>(query.getPage(), query.getSize());
        IPage<Supplier> supplierPage = supplierMapper.selectPage(page, wrapper);
        
        // 转换为VO
        List<SupplierVO> voList = supplierConvert.toVOList(supplierPage.getRecords());
        
        Page<SupplierVO> resultPage = new Page<>(supplierPage.getCurrent(), supplierPage.getSize(), supplierPage.getTotal());
        resultPage.setRecords(voList);
        
        return resultPage;
    }

    /**
     * 查询供应商详情
     */
    public SupplierDetailVO getDetail(String supplierId) {
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null) {
            throw new RRException("供应商不存在");
        }
        
        SupplierDetailVO detailVO = supplierConvert.toDetailVO(supplier);
        
        // 查询关联货物
        List<SupplierGoodsVO> goodsList = getSupplierGoods(supplierId);
        detailVO.setGoodsList(goodsList);
        
        return detailVO;
    }

    /**
     * 创建供应商
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(SupplierCreateCommand command) {
        // 检查名称是否重复
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getSupplierName, command.getSupplierName());
        Long count = supplierMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RRException("供应商名称已存在");
        }
        
        Supplier supplier = supplierConvert.toEntity(command);
        supplier.setCreateTime(new Date());
        supplier.setUpdateTime(new Date());
        
        supplierMapper.insert(supplier);
        log.info("创建供应商成功: supplierId={}, supplierName={}", supplier.getSupplierId(), supplier.getSupplierName());
    }

    /**
     * 更新供应商
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(SupplierUpdateCommand command) {
        Supplier supplier = supplierMapper.selectById(command.getSupplierId());
        if (supplier == null) {
            throw new RRException("供应商不存在");
        }
        
        // 检查名称是否重复（排除自己）
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getSupplierName, command.getSupplierName())
               .ne(Supplier::getSupplierId, command.getSupplierId());
        Long count = supplierMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RRException("供应商名称已存在");
        }
        
        supplierConvert.updateEntity(supplier, command);
        supplier.setUpdateTime(new Date());
        
        supplierMapper.updateById(supplier);
        log.info("更新供应商成功: supplierId={}", command.getSupplierId());
    }

    /**
     * 删除供应商
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String supplierId) {
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null) {
            throw new RRException("供应商不存在");
        }
        
        // 删除关联关系
        supplierGoodsRelMapper.deleteBySupplierId(supplierId);
        
        // 删除供应商
        supplierMapper.deleteById(supplierId);
        log.info("删除供应商成功: supplierId={}", supplierId);
    }

    /**
     * 获取供应商下拉选项
     */
    public List<SupplierVO> getOptions() {
        List<Supplier> suppliers = supplierMapper.selectActiveSuppliers();
        return supplierConvert.toVOList(suppliers);
    }

    /**
     * 获取供应商关联的货物列表
     */
    public List<SupplierGoodsVO> getSupplierGoods(String supplierId) {
        // 查询关联关系
        List<SupplierGoodsRel> relList = supplierGoodsRelMapper.selectBySupplierId(supplierId);
        if (relList.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取货物ID列表
        List<String> goodsIds = relList.stream()
                .map(SupplierGoodsRel::getGoodsId)
                .collect(Collectors.toList());
        
        // 查询货物信息
        List<Goods> goodsList = goodsMapper.selectBatchIds(goodsIds);
        
        // 构建VO
        List<SupplierGoodsVO> result = new ArrayList<>();
        for (SupplierGoodsRel rel : relList) {
            goodsList.stream()
                    .filter(g -> g.getGoodsId().equals(rel.getGoodsId()))
                    .findFirst()
                    .ifPresent(goods -> {
                        SupplierGoodsVO vo = supplierConvert.toSupplierGoodsVO(goods, rel);
                        result.add(vo);
                    });
        }
        
        return result;
    }

    /**
     * 供应商绑定货物
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindGoods(SupplierBindGoodsCommand command) {
        // 检查供应商是否存在
        Supplier supplier = supplierMapper.selectById(command.getSupplierId());
        if (supplier == null) {
            throw new RRException("供应商不存在");
        }
        
        // 查询已绑定的货物
        List<SupplierGoodsRel> existingRels = supplierGoodsRelMapper.selectBySupplierId(command.getSupplierId());
        List<String> existingGoodsIds = existingRels.stream()
                .map(SupplierGoodsRel::getGoodsId)
                .collect(Collectors.toList());
        
        // 过滤出需要新增的货物
        List<String> newGoodsIds = command.getGoodsIds().stream()
                .filter(goodsId -> !existingGoodsIds.contains(goodsId))
                .collect(Collectors.toList());
        
        if (newGoodsIds.isEmpty()) {
            return;
        }
        
        // 检查货物是否存在
        List<Goods> goodsList = goodsMapper.selectBatchIds(newGoodsIds);
        if (goodsList.size() != newGoodsIds.size()) {
            throw new RRException("部分货物不存在");
        }
        
        // 批量插入关联关系
        List<SupplierGoodsRel> relList = newGoodsIds.stream()
                .map(goodsId -> {
                    SupplierGoodsRel rel = new SupplierGoodsRel();
                    rel.setSupplierId(command.getSupplierId());
                    rel.setGoodsId(goodsId);
                    rel.setCreateTime(new Date());
                    rel.setUpdateTime(new Date());
                    return rel;
                })
                .collect(Collectors.toList());
        
        // 逐个插入（避免批量插入问题）
        for (SupplierGoodsRel rel : relList) {
            supplierGoodsRelMapper.insert(rel);
        }
        
        log.info("供应商绑定货物成功: supplierId={}, goodsCount={}", command.getSupplierId(), relList.size());
    }

    /**
     * 供应商解绑货物
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbindGoods(SupplierUnbindGoodsCommand command) {
        // 检查供应商是否存在
        Supplier supplier = supplierMapper.selectById(command.getSupplierId());
        if (supplier == null) {
            throw new RRException("供应商不存在");
        }
        
        // 删除关联关系
        supplierGoodsRelMapper.deleteBySupplierIdAndGoodsId(command.getSupplierId(), command.getGoodsId());
        
        log.info("供应商解绑货物成功: supplierId={}, goodsId={}", command.getSupplierId(), command.getGoodsId());
    }
}
