package com.suke.czx.modules.warehouse.stocktake.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.warehouse.stocktake.domain.entity.StocktakeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 盘点记录Mapper接口
 */
@Mapper
public interface StocktakeRecordMapper extends BaseMapper<StocktakeRecord> {
}
