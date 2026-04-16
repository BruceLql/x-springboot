package com.suke.czx.modules.warehouse.inventory.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.warehouse.inventory.domain.entity.InventoryEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 库存事件流水Mapper接口
 */
@Mapper
public interface InventoryEventMapper extends BaseMapper<InventoryEvent> {

    /**
     * 根据幂等键查询事件
     */
    InventoryEvent selectByIdempotentKey(@Param("idempotentKey") String idempotentKey);
}
