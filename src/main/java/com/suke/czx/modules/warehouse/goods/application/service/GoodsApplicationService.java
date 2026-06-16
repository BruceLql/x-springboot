package com.suke.czx.modules.warehouse.goods.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.modules.warehouse.goods.domain.entity.Goods;
import com.suke.czx.modules.warehouse.goods.infrastructure.convert.GoodsConvert;
import com.suke.czx.modules.warehouse.goods.infrastructure.repository.GoodsMapper;
import com.suke.czx.modules.warehouse.inventory.application.service.InventoryService;
import com.suke.czx.modules.warehouse.inventory.domain.entity.GoodsInventory;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.GoodsInventoryQueryDTO;
import com.suke.czx.modules.warehouse.goods.interfaces.dto.command.GoodsCreateCommand;
import com.suke.czx.modules.warehouse.goods.interfaces.dto.command.GoodsUpdateCommand;
import com.suke.czx.modules.warehouse.goods.interfaces.dto.query.GoodsPageQuery;
import com.suke.czx.modules.warehouse.goods.interfaces.vo.GoodsDetailVO;
import com.suke.czx.modules.warehouse.goods.interfaces.vo.GoodsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 货物应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsApplicationService {

    private final GoodsMapper goodsMapper;
    private final GoodsConvert goodsConvert;
    private final InventoryService inventoryService;

    /**
     * 分页查询货物列表
     */
    public IPage<GoodsVO> queryPage(GoodsPageQuery query) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        
        // 模糊查询货物名称
        if (StringUtils.isNotBlank(query.getGoodsName())) {
            wrapper.like(Goods::getGoodsName, query.getGoodsName());
        }
        
        // 类目筛选
        if (StringUtils.isNotBlank(query.getCategory())) {
            wrapper.eq(Goods::getCategory, query.getCategory());
        }
        
        // 商品分类筛选
        if (StringUtils.isNotBlank(query.getProductType())) {
            wrapper.eq(Goods::getProductType, query.getProductType());
        }
        
        // 状态筛选
        if (query.getStatus() != null) {
            wrapper.eq(Goods::getStatus, query.getStatus());
        }
        
        // 排序
        if (StringUtils.isNotBlank(query.getOrderBy())) {
            boolean isAsc = "asc".equalsIgnoreCase(query.getOrderDirection());
            wrapper.orderBy(true, isAsc, Goods::getCreateTime);
        } else {
            wrapper.orderByDesc(Goods::getCreateTime);
        }
        
        Page<Goods> page = new Page<>(query.getPage(), query.getSize());
        IPage<Goods> goodsPage = goodsMapper.selectPage(page, wrapper);
        
        // 转换为VO
        List<GoodsVO> voList = goodsConvert.toVOList(goodsPage.getRecords());
        
        Page<GoodsVO> resultPage = new Page<>(goodsPage.getCurrent(), goodsPage.getSize(), goodsPage.getTotal());
        resultPage.setRecords(voList);
        
        return resultPage;
    }

    /**
     * 查询货物详情
     */
    public GoodsDetailVO getDetail(String goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new RRException("货物不存在");
        }
        return goodsConvert.toDetailVO(goods);
    }

    /**
     * 根据ID查询货物
     */
    public Goods getById(String goodsId) {
        return goodsMapper.selectById(goodsId);
    }

    /**
     * 创建货物
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(GoodsCreateCommand command) {
        // 检查名称是否重复
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getGoodsName, command.getGoodsName());
        Long count = goodsMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RRException("货物名称已存在");
        }
        
        Goods goods = goodsConvert.toEntity(command);
        goods.setCreateTime(new Date());
        goods.setUpdateTime(new Date());
        
        goodsMapper.insert(goods);
        log.info("创建货物成功: goodsId={}, goodsName={}", goods.getGoodsId(), goods.getGoodsName());
    }

    /**
     * 更新货物
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(GoodsUpdateCommand command) {
        Goods goods = goodsMapper.selectById(command.getGoodsId());
        if (goods == null) {
            throw new RRException("货物不存在");
        }
        
        // 检查名称是否重复（排除自己）
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getGoodsName, command.getGoodsName())
               .ne(Goods::getGoodsId, command.getGoodsId());
        Long count = goodsMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RRException("货物名称已存在");
        }
        
        goodsConvert.updateEntity(goods, command);
        goods.setUpdateTime(new Date());
        
        goodsMapper.updateById(goods);
        log.info("更新货物成功: goodsId={}", command.getGoodsId());
    }

    /**
     * 删除货物
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new RRException("货物不存在");
        }

        // 检查库存关联：如果存在库存记录且库存不为零，禁止删除
        GoodsInventoryQueryDTO queryDTO = new GoodsInventoryQueryDTO();
        queryDTO.setGoodsId(goodsId);
        GoodsInventory inventory = inventoryService.getGoodsInventory(queryDTO);
        if (inventory != null) {
            if (inventory.getRealQuantity() > 0 || inventory.getWithholdQuantity() > 0 || inventory.getOccupyQuantity() > 0) {
                throw new RRException("货物存在库存记录，无法删除，请先清零库存");
            }
        }

        goodsMapper.deleteById(goodsId);
        log.info("删除货物成功: goodsId={}", goodsId);
    }

    /**
     * 获取货物下拉选项
     */
    public List<GoodsVO> getOptions() {
        List<Goods> goodsList = goodsMapper.selectActiveGoods();
        return goodsConvert.toVOList(goodsList);
    }

    /**
     * 根据供应商ID查询关联货物
     */
    public List<GoodsVO> getBySupplierId(String supplierId) {
        List<Goods> goodsList = goodsMapper.selectBySupplierId(supplierId);
        return goodsConvert.toVOList(goodsList);
    }
}
