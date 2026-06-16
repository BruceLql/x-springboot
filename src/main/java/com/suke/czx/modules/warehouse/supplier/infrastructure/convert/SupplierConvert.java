package com.suke.czx.modules.warehouse.supplier.infrastructure.convert;

import com.suke.czx.modules.warehouse.goods.domain.entity.Goods;
import com.suke.czx.modules.warehouse.supplier.domain.entity.Supplier;
import com.suke.czx.modules.warehouse.supplier.domain.entity.SupplierGoodsRel;
import com.suke.czx.modules.warehouse.supplier.interfaces.dto.command.SupplierCreateCommand;
import com.suke.czx.modules.warehouse.supplier.interfaces.dto.command.SupplierUpdateCommand;
import com.suke.czx.modules.warehouse.supplier.interfaces.vo.SupplierDetailVO;
import com.suke.czx.modules.warehouse.supplier.interfaces.vo.SupplierGoodsVO;
import com.suke.czx.modules.warehouse.supplier.interfaces.vo.SupplierVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 供应商对象转换器
 */
@Mapper(componentModel = "spring")
public interface SupplierConvert {

    SupplierConvert INSTANCE = Mappers.getMapper(SupplierConvert.class);

    /**
     * Entity -> VO
     */
    @Mapping(target = "supplierTypeDesc", expression = "java(getSupplierTypeDesc(supplier.getSupplierType()))")
    @Mapping(target = "statusDesc", expression = "java(getStatusDesc(supplier.getStatus()))")
    SupplierVO toVO(Supplier supplier);

    /**
     * Entity列表 -> VO列表
     */
    List<SupplierVO> toVOList(List<Supplier> suppliers);

    /**
     * Entity -> DetailVO
     */
    @Mapping(target = "supplierTypeDesc", expression = "java(getSupplierTypeDesc(supplier.getSupplierType()))")
    @Mapping(target = "statusDesc", expression = "java(getStatusDesc(supplier.getStatus()))")
    SupplierDetailVO toDetailVO(Supplier supplier);

    /**
     * CreateCommand -> Entity
     */
    @Mapping(target = "supplierId", ignore = true)
    @Mapping(target = "status", constant = "1")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    Supplier toEntity(SupplierCreateCommand command);

    /**
     * UpdateCommand -> Entity（更新现有实体）
     */
    @Mapping(target = "supplierId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void updateEntity(@MappingTarget Supplier supplier, SupplierUpdateCommand command);

    /**
     * 转换供应商关联的货物列表（优化版：使用Map提升性能）
     * 注意：此方法应在Application Service层调用，符合DDD分层规范
     */
    default List<SupplierGoodsVO> convertToSupplierGoodsVOList(List<SupplierGoodsRel> relList, List<Goods> goodsList) {
        if (relList == null || relList.isEmpty()) {
            return List.of();
        }
        if (goodsList == null || goodsList.isEmpty()) {
            return List.of();
        }

        // 将goodsList转为Map，避免N+1查询问题，时间复杂度从O(n*m)优化为O(n+m)
        Map<String, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getGoodsId, goods -> goods, (a, b) -> a));

        return relList.stream()
                .map(rel -> {
                    Goods goods = goodsMap.get(rel.getGoodsId());
                    return toSupplierGoodsVO(goods, rel);
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Goods + SupplierGoodsRel -> SupplierGoodsVO
     */
    default SupplierGoodsVO toSupplierGoodsVO(Goods goods, SupplierGoodsRel rel) {
        if (goods == null) {
            return null;
        }
        return SupplierGoodsVO.builder()
                .id(rel.getId())
                .goodsId(goods.getGoodsId())
                .goodsName(goods.getGoodsName())
                .goodsImage(goods.getGoodsImage())
                .height(goods.getHeight())
                .weight(goods.getWeight())
                .category(goods.getCategory())
                .productType(goods.getProductType() != null ? goods.getProductType().getValue() : null)
                .productTypeDesc(goods.getProductType() != null ? goods.getProductType().getDesc() : null)
                .build();
    }

    /**
     * 获取供应商类型描述
     */
    @Named("supplierTypeDesc")
    default String getSupplierTypeDesc(String supplierType) {
        if (supplierType == null) {
            return "";
        }
        return switch (supplierType) {
            case "FACTORY" -> "源头厂家";
            case "DEALER" -> "二道贩子";
            default -> supplierType;
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

    /**
     * 获取商品分类描述
     */
    @Named("productTypeDesc")
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
}
