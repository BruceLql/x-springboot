package com.suke.czx.modules.warehouse.inbound.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.warehouse.inbound.domain.entity.InboundItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 入库明细Mapper接口
 */
@Mapper
public interface InboundItemMapper extends BaseMapper<InboundItem> {

    /**
     * 根据入库单ID查询明细列表
     */
    List<InboundItem> selectByOrderId(@Param("orderId") String orderId);

    /**
     * 根据批次号查询明细列表
     */
    List<InboundItem> selectByBatchNo(@Param("batchNo") String batchNo);
}
