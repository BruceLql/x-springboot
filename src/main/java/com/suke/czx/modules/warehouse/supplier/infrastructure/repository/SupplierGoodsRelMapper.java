package com.suke.czx.modules.warehouse.supplier.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.warehouse.supplier.domain.entity.SupplierGoodsRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 供应商货物关联Mapper接口
 */
@Mapper
public interface SupplierGoodsRelMapper extends BaseMapper<SupplierGoodsRel> {

    /**
     * 根据供应商ID查询关联记录
     */
    List<SupplierGoodsRel> selectBySupplierId(@Param("supplierId") String supplierId);

    /**
     * 根据货物ID查询关联记录
     */
    List<SupplierGoodsRel> selectByGoodsId(@Param("goodsId") String goodsId);

    /**
     * 批量插入关联关系
     */
    void batchInsert(@Param("list") List<SupplierGoodsRel> list);

    /**
     * 根据供应商ID删除所有关联
     */
    void deleteBySupplierId(@Param("supplierId") String supplierId);

    /**
     * 根据供应商ID和货物ID删除关联
     */
    void deleteBySupplierIdAndGoodsId(@Param("supplierId") String supplierId, @Param("goodsId") String goodsId);
}
