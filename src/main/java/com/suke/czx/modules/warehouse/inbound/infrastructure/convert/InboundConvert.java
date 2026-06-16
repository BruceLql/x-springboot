package com.suke.czx.modules.warehouse.inbound.infrastructure.convert;

import com.suke.czx.modules.warehouse.inbound.domain.entity.InboundItem;
import com.suke.czx.modules.warehouse.inbound.interfaces.vo.InboundItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 入库对象转换器
 */
@Mapper(componentModel = "spring")
public interface InboundConvert {

    InboundConvert INSTANCE = Mappers.getMapper(InboundConvert.class);

    /**
     * InboundItem -> InboundItemVO (goodsName/goodsImage 由服务层批量注入)
     */
    @Mapping(target = "goodsName", ignore = true)
    @Mapping(target = "goodsImage", ignore = true)
    InboundItemVO toItemVO(InboundItem item);

    List<InboundItemVO> toItemVOList(List<InboundItem> items);
}
