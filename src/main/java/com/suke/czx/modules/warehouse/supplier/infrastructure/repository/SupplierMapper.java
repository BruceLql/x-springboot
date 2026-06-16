package com.suke.czx.modules.warehouse.supplier.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.warehouse.supplier.domain.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 供应商Mapper接口
 */
@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {

    /**
     * 查询启用的供应商列表（用于下拉选择）
     */
    List<Supplier> selectActiveSuppliers();

    /**
     * 根据货物ID查询关联的供应商列表
     */
    List<Supplier> selectByGoodsId(@Param("goodsId") String goodsId);
}
