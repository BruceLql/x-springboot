package com.suke.czx.modules.warehouse.outbound.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.common.utils.LeafSnowflakeGenerator;
import com.suke.czx.common.utils.UserUtil;
import com.suke.czx.modules.warehouse.inventory.application.service.InventoryService;
import com.suke.czx.modules.warehouse.inventory.domain.command.DamageOutboundDTO;
import com.suke.czx.modules.warehouse.outbound.domain.entity.OutboundGoodsRecord;
import com.suke.czx.modules.warehouse.outbound.infrastructure.repository.OutboundGoodsRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 出库应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundApplicationService {

    private final OutboundGoodsRecordMapper outboundGoodsRecordMapper;
    private final InventoryService inventoryService;
    private final LeafSnowflakeGenerator leafSnowflakeGenerator;

    /**
     * 分页查询出库记录
     */
    public IPage<OutboundGoodsRecord> queryPage(String orderType, String sourceId, Long page, Long size) {
        LambdaQueryWrapper<OutboundGoodsRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isNotBlank(orderType)) {
            wrapper.eq(OutboundGoodsRecord::getOrderType, orderType);
        }
        if (StringUtils.isNotBlank(sourceId)) {
            wrapper.eq(OutboundGoodsRecord::getSourceId, sourceId);
        }
        
        wrapper.orderByDesc(OutboundGoodsRecord::getCreateTime);
        
        Page<OutboundGoodsRecord> pageParam = new Page<>(page, size);
        return outboundGoodsRecordMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 报损出库
     */
    @Transactional(rollbackFor = Exception.class)
    public String damageOutbound(String goodsId, Integer quantity, String reason) {
        if (quantity <= 0) {
            throw new RRException("出库数量必须大于0");
        }
        
        String operatorId = UserUtil.getUserId();
        Integer tenancyId = UserUtil.getUserTenancyId();
        
        // 扣减库存
        String orderNo = generateOrderNo("DM");
        DamageOutboundDTO damageDTO = new DamageOutboundDTO();
        damageDTO.setGoodsId(goodsId);
        damageDTO.setQuantity(quantity);
        damageDTO.setOrderNo(orderNo);
        damageDTO.setOperatorId(operatorId);
        damageDTO.setTenancyId(tenancyId);
        inventoryService.damageOutbound(damageDTO);
        
        // 保存出库记录
        OutboundGoodsRecord record = new OutboundGoodsRecord();
        record.setOrderId(leafSnowflakeGenerator.nextIdStr());
        record.setOrderType("DAMAGE");
        record.setSourceType("GOODS");
        record.setSourceId(goodsId);
        record.setQuantity(quantity);
        record.setReason(reason);
        record.setRelatedOrderNo(orderNo);
        record.setStatus(1);
        record.setOperatorId(operatorId);
        record.setTenancyId(tenancyId);
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        
        outboundGoodsRecordMapper.insert(record);
        
        log.info("报损出库成功: goodsId={}, quantity={}", goodsId, quantity);
        return orderNo;
    }

    /**
     * 生成单号
     */
    private String generateOrderNo(String prefix) {
        return prefix + cn.hutool.core.date.DateUtil.format(new Date(), "yyyyMMddHHmmss") + leafSnowflakeGenerator.nextId();
    }
}