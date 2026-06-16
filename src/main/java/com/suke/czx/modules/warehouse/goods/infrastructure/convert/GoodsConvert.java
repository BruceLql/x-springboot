package com.suke.czx.modules.warehouse.goods.infrastructure.convert;

import com.suke.czx.modules.warehouse.goods.domain.entity.Goods;
import com.suke.czx.modules.warehouse.goods.domain.enums.ProductTypeEnum;
import com.suke.czx.modules.warehouse.goods.domain.enums.StatusEnum;
import com.suke.czx.modules.warehouse.goods.interfaces.dto.command.GoodsCreateCommand;
import com.suke.czx.modules.warehouse.goods.interfaces.dto.command.GoodsUpdateCommand;
import com.suke.czx.modules.warehouse.goods.interfaces.vo.GoodsDetailVO;
import com.suke.czx.modules.warehouse.goods.interfaces.vo.GoodsInventoryVO;
import com.suke.czx.modules.warehouse.goods.interfaces.vo.GoodsVO;
import com.suke.czx.modules.warehouse.inventory.domain.entity.GoodsInventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 货物对象转换器
 */
@Mapper(componentModel = "spring")
public interface GoodsConvert {

    GoodsConvert INSTANCE = Mappers.getMapper(GoodsConvert.class);

    /**
     * Entity -> VO
     */
    @Mapping(target = "productTypeDesc", expression = "java(getProductTypeDesc(goods.getProductType()))")
    @Mapping(target = "statusDesc", expression = "java(goods.getStatus().getDesc())")
    @Mapping(target = "status", expression = "java(goods.getStatus().getCode())")
    GoodsVO toVO(Goods goods);

    /**
     * Entity列表 -> VO列表
     */
    List<GoodsVO> toVOList(List<Goods> goodsList);

    default List<GoodsInventoryVO> toVOList(List<Goods> goodsList, List<GoodsInventory> goodsInventoryVOList) {
        Map<String, Goods> goodsMap = goodsList.stream().collect(Collectors.toMap(Goods::getGoodsId, Function.identity()));
        return goodsInventoryVOList.stream().map(x -> toVO(goodsMap.get(x.getGoodsId()), x)).filter(x -> x != null).toList();
    }

    default GoodsInventoryVO toVO(Goods goods, GoodsInventory goodsInventory) {
        if (goods == null) {
            return null;
        }
        return GoodsInventoryVO.builder()
                .goodsId(goods.getGoodsId())
                .goodsName(goods.getGoodsName())
                .goodsImage(goods.getGoodsImage())
                .height(goods.getHeight())
                .weight(goods.getWeight())
                .category(goods.getCategory())
                .productType(goods.getProductType() != null ? goods.getProductType().getCode() : null)
                .inventoryId(goodsInventory.getInventoryId())
                .quantity(goodsInventory.getSellableQuantity())
                .status(goods.getStatus() != null ? goods.getStatus().getCode() : null)
                .build();
    }

    /**
     * Entity -> DetailVO
     */
    @Mapping(target = "productTypeDesc", expression = "java(getProductTypeDesc(goods.getProductType()))")
    @Mapping(target = "statusDesc", expression = "java(getStatusDesc(goods.getStatus()))")
    @Mapping(target = "status", expression = "java(getStatus(goods.getStatus()))")
    GoodsDetailVO toDetailVO(Goods goods);

    /**
     * CreateCommand -> Entity
     */
    @Mapping(target = "goodsId", ignore = true)
    @Mapping(target = "status", constant = "ENABLED")
    @Mapping(target = "tenancyId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "productType", expression = "java(convertProductTypeToEnum(command.getProductType()))")
    Goods toEntity(GoodsCreateCommand command);

    /**
     * UpdateCommand -> Entity（更新现有实体）
     */
    @Mapping(target = "goodsId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tenancyId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "productType", expression = "java(convertProductTypeToEnum(command.getProductType()))")
    void updateEntity(@MappingTarget Goods goods, GoodsUpdateCommand command);

    /**
     * 获取商品分类描述
     */
    default String getProductTypeDesc(ProductTypeEnum productType) {
        if (productType == null) {
            return "";
        }
        return productType.getDesc();
    }

    /**
     * 商品分类转换（支持前端传中文或英文）
     */
    default ProductTypeEnum convertProductTypeToEnum(String productType) {
        if (productType == null) {
            return null;
        }
        ProductTypeEnum enumFromCode = ProductTypeEnum.fromCode(productType);
        if (enumFromCode != null) {
            return enumFromCode;
        }
        return ProductTypeEnum.fromDesc(productType);
    }

    /**
     * 获取状态描述
     */
    default String getStatusDesc(StatusEnum status) {
        if (status == null) {
            return "";
        }
        return status.getDesc();
    }

    /**
     * 获取状态描述
     */
    default Integer getStatus(StatusEnum status) {
        if (status == null) {
            return null;
        }
        return status.getCode();
    }
}
