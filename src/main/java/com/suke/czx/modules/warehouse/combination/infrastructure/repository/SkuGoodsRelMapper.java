package com.suke.czx.modules.warehouse.combination.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.warehouse.combination.domain.entity.SkuGoodsRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SKU货物关联Mapper接口
 */
@Mapper
public interface SkuGoodsRelMapper extends BaseMapper<SkuGoodsRel> {

    /**
     * 根据SKU ID查询关联列表
     */
    List<SkuGoodsRel> selectBySkuId(@Param("skuId") String skuId);

    /**
     * 根据SKU ID删除关联
     */
    void deleteBySkuId(@Param("skuId") String skuId);
}
