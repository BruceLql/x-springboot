package com.suke.czx.modules.warehouse.goods.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.warehouse.goods.domain.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 货物Mapper接口
 */
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 根据供应商ID查询关联的货物列表
     */
    List<Goods> selectBySupplierId(@Param("supplierId") String supplierId);

    /**
     * 查询启用的货物列表（用于下拉选择）
     */
    List<Goods> selectActiveGoods();
}
