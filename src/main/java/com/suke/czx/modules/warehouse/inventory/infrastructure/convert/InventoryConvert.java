package com.suke.czx.modules.warehouse.inventory.infrastructure.convert;

import com.suke.czx.modules.warehouse.inventory.domain.entity.GoodsInventory;
import com.suke.czx.modules.warehouse.inventory.interfaces.vo.GoodsInventoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 库存对象转换器
 */
@Mapper(componentModel = "spring")
public interface InventoryConvert {

    InventoryConvert INSTANCE = Mappers.getMapper(InventoryConvert.class);

    /**
     * Entity -> VO (goods关联字段由服务层批量注入)
     */
    @Mapping(target = "goodsName", ignore = true)
    @Mapping(target = "goodsImage", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "productType", ignore = true)
    @Mapping(target = "productTypeDesc", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "statusDesc", ignore = true)
    GoodsInventoryVO toVO(GoodsInventory inventory);

    List<GoodsInventoryVO> toVOList(List<GoodsInventory> inventoryList);
}
