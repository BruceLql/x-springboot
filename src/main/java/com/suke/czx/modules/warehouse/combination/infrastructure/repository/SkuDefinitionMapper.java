package com.suke.czx.modules.warehouse.combination.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.warehouse.combination.domain.entity.SkuDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SKU定义Mapper接口
 */
@Mapper
public interface SkuDefinitionMapper extends BaseMapper<SkuDefinition> {

    /**
     * 根据状态查询SKU列表
     */
    List<SkuDefinition> selectByStatus(@Param("status") Integer status);
}
