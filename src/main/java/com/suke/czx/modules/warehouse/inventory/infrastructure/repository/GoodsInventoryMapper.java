package com.suke.czx.modules.warehouse.inventory.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.warehouse.inventory.domain.entity.GoodsInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 货物库存Mapper接口
 */
@Mapper
public interface GoodsInventoryMapper extends BaseMapper<GoodsInventory> {

    /**
     * 根据货物ID查询库存
     */
    GoodsInventory selectByGoodsId(@Param("goodsId") String goodsId);

    /**
     * 增加实际库存（入库）
     */
    int increaseRealQuantity(@Param("goodsId") String goodsId, @Param("quantity") int quantity, @Param("version") int version);

    /**
     * 锁定库存（组商品）
     */
    int withholdQuantity(@Param("goodsId") String goodsId, @Param("quantity") int quantity, @Param("version") int version);

    /**
     * 释放锁定库存
     */
    int releaseWithhold(@Param("goodsId") String goodsId, @Param("quantity") int quantity, @Param("version") int version);

    /**
     * 占用库存（布款完成）
     */
    int occupyQuantity(@Param("goodsId") String goodsId, @Param("quantity") int quantity, @Param("version") int version);

    /**
     * 取消占用库存
     */
    int cancelOccupy(@Param("goodsId") String goodsId, @Param("quantity") int quantity, @Param("version") int version);

    /**
     * 确认出库（扣减实际和占用库存）
     */
    int confirmOutbound(@Param("goodsId") String goodsId, @Param("quantity") int quantity, @Param("version") int version);

    /**
     * 报损出库（直接扣减实际库存）
     */
    int damageOutbound(@Param("goodsId") String goodsId, @Param("quantity") int quantity, @Param("version") int version);
}
