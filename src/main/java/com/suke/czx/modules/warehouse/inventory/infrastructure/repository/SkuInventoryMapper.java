package com.suke.czx.modules.warehouse.inventory.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suke.czx.modules.warehouse.inventory.domain.entity.SkuInventory;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.SkuInventoryPageQuery;
import com.suke.czx.modules.warehouse.inventory.interfaces.vo.SkuInventoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SKU库存Mapper接口
 */
@Mapper
public interface SkuInventoryMapper extends BaseMapper<SkuInventory> {

    /**
     * 分页查询SKU库存（关联SKU定义信息）
     */
    IPage<SkuInventoryVO> selectSkuInventoryPage(Page<SkuInventoryVO> page, @Param("query") SkuInventoryPageQuery query);

    /**
     * 更新SKU库存
     */
    int updateSkuInventory(@Param("skuId") String skuId, @Param("quantity") int quantity, @Param("version") int version);

}
