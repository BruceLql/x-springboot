package com.suke.czx.modules.warehouse.goods.infrastructure.convert;

import com.suke.czx.modules.warehouse.goods.domain.entity.Goods;
import com.suke.czx.modules.warehouse.goods.interfaces.dto.command.GoodsCreateCommand;
import com.suke.czx.modules.warehouse.goods.interfaces.dto.command.GoodsUpdateCommand;
import com.suke.czx.modules.warehouse.goods.interfaces.vo.GoodsDetailVO;
import com.suke.czx.modules.warehouse.goods.interfaces.vo.GoodsVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

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
    @Mapping(target = "statusDesc", expression = "java(getStatusDesc(goods.getStatus()))")
    GoodsVO toVO(Goods goods);

    /**
     * Entity列表 -> VO列表
     */
    List<GoodsVO> toVOList(List<Goods> goodsList);

    /**
     * Entity -> DetailVO
     */
    @Mapping(target = "productTypeDesc", expression = "java(getProductTypeDesc(goods.getProductType()))")
    @Mapping(target = "statusDesc", expression = "java(getStatusDesc(goods.getStatus()))")
    GoodsDetailVO toDetailVO(Goods goods);

    /**
     * CreateCommand -> Entity
     */
    @Mapping(target = "goodsId", ignore = true)
    @Mapping(target = "status", constant = "1")
    @Mapping(target = "tenancyId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    Goods toEntity(GoodsCreateCommand command);

    /**
     * UpdateCommand -> Entity（更新现有实体）
     */
    @Mapping(target = "goodsId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tenancyId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void updateEntity(@MappingTarget Goods goods, GoodsUpdateCommand command);

    /**
     * 获取商品分类描述
     */
    default String getProductTypeDesc(String productType) {
        if (productType == null) {
            return "";
        }
        return switch (productType) {
            case "MALE" -> "男款";
            case "FEMALE" -> "女款";
            case "CHILDREN" -> "儿童款";
            default -> productType;
        };
    }

    /**
     * 获取状态描述
     */
    default String getStatusDesc(Integer status) {
        if (status == null) {
            return "";
        }
        return status == 1 ? "启用" : "禁用";
    }
}
