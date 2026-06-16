package com.suke.czx.modules.warehouse.outbound.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.warehouse.outbound.domain.entity.OutboundGoodsRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 货物出库记录Mapper接口
 */
@Mapper
public interface OutboundGoodsRecordMapper extends BaseMapper<OutboundGoodsRecord> {
}
