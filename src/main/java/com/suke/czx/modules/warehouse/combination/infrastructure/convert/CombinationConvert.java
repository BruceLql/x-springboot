package com.suke.czx.modules.warehouse.combination.infrastructure.convert;

import com.suke.czx.modules.warehouse.combination.domain.entity.SkuDefinition;
import com.suke.czx.modules.warehouse.combination.interfaces.dto.command.CombinationCreateCommand;
import com.suke.czx.modules.warehouse.combination.interfaces.vo.CombinationDetailVO;
import com.suke.czx.modules.warehouse.combination.interfaces.vo.CombinationGoodsItemVO;
import com.suke.czx.modules.warehouse.combination.interfaces.vo.CombinationVO;
import com.suke.czx.modules.warehouse.goods.domain.entity.Goods;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * SKU搭配对象转换器
 */
@Mapper(componentModel = "spring")
public interface CombinationConvert {

    CombinationConvert INSTANCE = Mappers.getMapper(CombinationConvert.class);

    /**
     * Entity -> VO
     */
    @Mapping(target = "statusDesc", expression = "java(getStatusDesc(skuDefinition.getStatus()))")
    CombinationVO toVO(SkuDefinition skuDefinition);

    /**
     * Entity列表 -> VO列表
     */
    List<CombinationVO> toVOList(List<SkuDefinition> skuDefinitionList);

    /**
     * Entity -> DetailVO
     */
    @Mapping(target = "statusDesc", expression = "java(getStatusDesc(skuDefinition.getStatus()))")
    @Mapping(target = "goodsList", ignore = true)
    CombinationDetailVO toDetailVO(SkuDefinition skuDefinition);

    CombinationGoodsItemVO convert(Goods goods);

    List<CombinationGoodsItemVO> convert(List<Goods> goodsList);

    /**
     * CreateCommand -> Entity
     */
    @Mapping(target = "skuId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "goodsIds", ignore = true)
    @Mapping(target = "operatorId", ignore = true)
    @Mapping(target = "tenancyId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    SkuDefinition toEntity(CombinationCreateCommand command);

    /**
     * 获取状态描述
     */
    default String getStatusDesc(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "已取消";
            case 1 -> "待布款";
            case 2 -> "已布款";
            case 3 -> "布款回滚";
            default -> String.valueOf(status);
        };
    }
}
