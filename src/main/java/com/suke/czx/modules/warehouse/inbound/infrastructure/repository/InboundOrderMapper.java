package com.suke.czx.modules.warehouse.inbound.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.warehouse.inbound.domain.entity.InboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 入库单Mapper接口
 */
@Mapper
public interface InboundOrderMapper extends BaseMapper<InboundOrder> {

    /**
     * 根据批次号查询入库单列表
     */
    List<InboundOrder> selectByBatchNo(@Param("batchNo") String batchNo);
}
