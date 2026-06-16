package com.suke.czx.modules.warehouse.inventory.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.modules.warehouse.inventory.domain.entity.GoodsInventory;
import com.suke.czx.modules.warehouse.inventory.domain.entity.InventoryEvent;
import com.suke.czx.modules.warehouse.inventory.infrastructure.repository.GoodsInventoryMapper;
import com.suke.czx.modules.warehouse.inventory.infrastructure.repository.InventoryEventMapper;
import com.suke.czx.modules.warehouse.inventory.infrastructure.repository.SkuInventoryMapper;
import com.suke.czx.modules.warehouse.inventory.domain.entity.SkuInventory;
import com.suke.czx.modules.warehouse.inventory.domain.command.*;

import cn.hutool.core.util.StrUtil;
import com.suke.czx.common.utils.LeafSnowflakeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 库存领域服务
 * 提供核心的库存操作，确保数据一致性
 * 乐观锁冲突时自动重试（最多3次），确保高并发场景下的可靠性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryDomainService {

    private static final int MAX_RETRY = 3;
    private static final String IDEMPOTENT_KEY_PREFIX = "x-springboot:inventory:idempotent:";
    private static final long IDEMPOTENT_TTL_MINUTES = 5;

    private final GoodsInventoryMapper goodsInventoryMapper;
    private final SkuInventoryMapper skuInventoryMapper;
    private final InventoryEventMapper inventoryEventMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final LeafSnowflakeGenerator leafSnowflakeGenerator;

    /**
     * 初始化货物库存
     */
    public void initGoodsInventory(InitInventoryDTO dto) {
        String goodsId = dto.getGoodsId();
        Integer tenancyId = dto.getTenancyId();
        GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
        if (inventory != null) {
            return;
        }

        inventory = new GoodsInventory();
        inventory.setInventoryId(leafSnowflakeGenerator.nextIdStr());
        inventory.setGoodsId(goodsId);
        inventory.setRealQuantity(0);
        inventory.setWithholdQuantity(0);
        inventory.setOccupyQuantity(0);
        inventory.setSellableQuantity(0);
        inventory.setVersion(0);
        inventory.setTenancyId(tenancyId);
        inventory.setCreateTime(new Date());
        inventory.setUpdateTime(new Date());

        goodsInventoryMapper.insert(inventory);
        log.info("初始化货物库存成功: goodsId={}", goodsId);
    }

    /**
     * 增加实际库存（入库）
     */
    public void increaseInventory(IncreaseInventoryDTO dto) {
        String goodsId = dto.getGoodsId();
        int quantity = dto.getQuantity();
        String orderNo = dto.getOrderNo();
        String operatorId = dto.getOperatorId();
        Integer tenancyId = dto.getTenancyId();

        if (quantity <= 0) {
            throw new RRException("入库数量必须大于0");
        }

        // 库存不存在时自动初始化
        GoodsInventory inventory = goodsInventoryMapper.selectByGoodsId(goodsId);
        if (inventory == null) {
            InitInventoryDTO initDTO = new InitInventoryDTO();
            initDTO.setGoodsId(goodsId);
            initDTO.setTenancyId(tenancyId);
            initGoodsInventory(initDTO);
        }

        // 更新库存（乐观锁 + 重试）
        int rows = retryOnOptimisticLock(goodsId, () -> {
            GoodsInventory inv = goodsInventoryMapper.selectByGoodsId(goodsId);
            if (inv == null) {
                throw new RRException("库存不存在，初始化可能失败");
            }
            return goodsInventoryMapper.increaseRealQuantity(goodsId, quantity, inv.getVersion());
        });
        if (rows == 0) {
            throw new RRException("库存更新失败，已重试" + MAX_RETRY + "次");
        }

        // 记录库存事件
        recordInventoryEvent(InventoryEventParam.builder()
                .businessType("INBOUND").sourceType("GOODS").sourceId(goodsId).eventType("INBOUND")
                .changeReal(quantity).changeWithhold(0).changeOccupy(0).changeSellable(quantity)
                .relatedOrderNo(orderNo).operatorId(operatorId).tenancyId(tenancyId)
                .build());

        log.info("货物入库成功: goodsId={}, quantity={}, orderNo={}", goodsId, quantity, orderNo);
    }

    /**
     * 锁定库存（组商品）
     */
    public void withholdInventory(WithholdInventoryDTO dto) {
        String goodsId = dto.getGoodsId();
        int quantity = dto.getQuantity();
        String skuId = dto.getSkuId();
        String operatorId = dto.getOperatorId();
        Integer tenancyId = dto.getTenancyId();

        if (quantity <= 0) {
            throw new RRException("锁定数量必须大于0");
        }

        // 更新货物库存（乐观锁 + 重试，lambda内含完整校验）
        int rows = retryOnOptimisticLock(goodsId, () -> {
            GoodsInventory inv = goodsInventoryMapper.selectByGoodsId(goodsId);
            if (inv == null) {
                throw new RRException("库存不存在");
            }
            if (inv.getSellableQuantity() < quantity) {
                throw new RRException("可售库存不足，无法锁定");
            }
            return goodsInventoryMapper.withholdQuantity(goodsId, quantity, inv.getVersion());
        });
        if (rows == 0) {
            throw new RRException("库存锁定失败，已重试" + MAX_RETRY + "次");
        }

        // 记录库存事件
        recordInventoryEvent(InventoryEventParam.builder()
                .businessType("SKU_COMBINATION").sourceType("GOODS").sourceId(goodsId).eventType("WITHHOLD")
                .changeReal(0).changeWithhold(quantity).changeOccupy(0).changeSellable(-quantity)
                .relatedOrderNo(skuId).operatorId(operatorId).tenancyId(tenancyId)
                .build());

        log.info("货物锁定库存成功: goodsId={}, quantity={}, skuId={}", goodsId, quantity, skuId);
    }

    /**
     * 释放锁定库存（取消组商品）
     */
    public void releaseWithhold(ReleaseWithholdDTO dto) {
        String goodsId = dto.getGoodsId();
        int quantity = dto.getQuantity();
        String skuId = dto.getSkuId();
        String operatorId = dto.getOperatorId();
        Integer tenancyId = dto.getTenancyId();

        if (quantity <= 0) {
            throw new RRException("释放数量必须大于0");
        }

        // 更新库存（乐观锁 + 重试，lambda内含完整校验）
        int rows = retryOnOptimisticLock(goodsId, () -> {
            GoodsInventory inv = goodsInventoryMapper.selectByGoodsId(goodsId);
            if (inv == null) {
                throw new RRException("库存不存在");
            }
            if (inv.getWithholdQuantity() < quantity) {
                throw new RRException("锁定库存不足，无法释放");
            }
            return goodsInventoryMapper.releaseWithhold(goodsId, quantity, inv.getVersion());
        });
        if (rows == 0) {
            throw new RRException("库存释放失败，已重试" + MAX_RETRY + "次");
        }

        // 记录库存事件
        recordInventoryEvent(InventoryEventParam.builder()
                .businessType("SKU_COMBINATION").sourceType("GOODS").sourceId(goodsId).eventType("CANCEL")
                .changeReal(0).changeWithhold(-quantity).changeOccupy(0).changeSellable(quantity)
                .relatedOrderNo(skuId).operatorId(operatorId).tenancyId(tenancyId)
                .build());

        log.info("货物释放锁定库存成功: goodsId={}, quantity={}, skuId={}", goodsId, quantity, skuId);
    }

    /**
     * 占用库存（布款完成）
     */
    public void occupyInventory(OccupyInventoryDTO dto) {
        String goodsId = dto.getGoodsId();
        int quantity = dto.getQuantity();
        String skuId = dto.getSkuId();
        String operatorId = dto.getOperatorId();
        Integer tenancyId = dto.getTenancyId();

        if (quantity <= 0) {
            throw new RRException("占用数量必须大于0");
        }

        // 更新库存（乐观锁 + 重试，lambda内含完整校验）
        int rows = retryOnOptimisticLock(goodsId, () -> {
            GoodsInventory inv = goodsInventoryMapper.selectByGoodsId(goodsId);
            if (inv == null) {
                throw new RRException("库存不存在");
            }
            if (inv.getWithholdQuantity() < quantity) {
                throw new RRException("锁定库存不足，无法占用");
            }
            return goodsInventoryMapper.occupyQuantity(goodsId, quantity, inv.getVersion());
        });
        if (rows == 0) {
            throw new RRException("库存占用失败，已重试" + MAX_RETRY + "次");
        }

        // 记录库存事件
        recordInventoryEvent(InventoryEventParam.builder()
                .businessType("SKU_ARRANGEMENT").sourceType("GOODS").sourceId(goodsId).eventType("OCCUPY")
                .changeReal(0).changeWithhold(-quantity).changeOccupy(quantity).changeSellable(0)
                .relatedOrderNo(skuId).operatorId(operatorId).tenancyId(tenancyId)
                .build());

        log.info("货物占用库存成功: goodsId={}, quantity={}, skuId={}", goodsId, quantity, skuId);
    }

    /**
     * 批量占用库存（布款完成）
     **/
    public void occupyInventoryBatch(OccupyInventoryBatchDTO dto) {
        List<String> goodsIdList = dto.getGoodsIdList();
        int quantity = dto.getQuantity();
        String skuId = dto.getSkuId();
        String operatorId = dto.getOperatorId();
        Integer tenancyId = dto.getTenancyId();

        for (String goodsId : goodsIdList) {
            // 占用库存（乐观锁 + 重试，lambda内含完整校验）
            int rows = retryOnOptimisticLock(goodsId, () -> {
                GoodsInventory inv = goodsInventoryMapper.selectByGoodsId(goodsId);
                if (inv == null) {
                    throw new RRException("库存不存在: " + goodsId);
                }
                if (inv.getWithholdQuantity() < quantity) {
                    throw new RRException("占用库存不足，无法占用: " + goodsId);
                }
                return goodsInventoryMapper.occupyQuantity(goodsId, quantity, inv.getVersion());
            });
            if (rows == 0) {
                throw new RRException("库存占用失败，已重试" + MAX_RETRY + "次: " + goodsId);
            }
            // 记录库存事件
            recordInventoryEvent(InventoryEventParam.builder()
                    .businessType("SKU_ARRANGEMENT").sourceType("GOODS").sourceId(goodsId).eventType("OCCUPY")
                    .changeReal(0).changeWithhold(-quantity).changeOccupy(quantity).changeSellable(0)
                    .relatedOrderNo(skuId).operatorId(operatorId).tenancyId(tenancyId)
                    .build());
        }

        // 更新sku库存（乐观锁 + 重试，lambda内含完整校验）
        int skuRows = retryOnOptimisticLock(skuId, () -> {
            SkuInventory sku = skuInventoryMapper.selectOne(
                    new LambdaQueryWrapper<SkuInventory>().eq(SkuInventory::getSkuId, skuId)
            );
            if (sku == null) {
                throw new RRException("SKU库存不存在: " + skuId);
            }
            return skuInventoryMapper.updateSkuInventory(skuId, quantity, sku.getVersion());
        });
        if (skuRows == 0) {
            throw new RRException("SKU库存更新失败，已重试" + MAX_RETRY + "次");
        }

        // 记录库存事件
        recordInventoryEvent(InventoryEventParam.builder()
                .businessType("SKU_ARRANGEMENT").sourceType("SKU").sourceId(skuId).eventType("OCCUPY")
                .changeReal(0).changeWithhold(-quantity).changeOccupy(quantity).changeSellable(0)
                .relatedOrderNo(skuId).operatorId(operatorId).tenancyId(tenancyId)
                .build());

        log.info("货物占用库存成功: goodsId={}, quantity={}, skuId={}", StrUtil.join(",", goodsIdList), quantity, skuId);
    }

    /**
     * 取消占用货物库存（布款回滚）
     */
    public void cancelOccupy(CancelOccupyDTO dto) {
        String goodsId = dto.getGoodsId();
        int quantity = dto.getQuantity();
        String skuId = dto.getSkuId();
        String operatorId = dto.getOperatorId();
        Integer tenancyId = dto.getTenancyId();

        if (quantity <= 0) {
            throw new RRException("取消占用数量必须大于0");
        }

        // 更新库存（乐观锁 + 重试，lambda内含完整校验）
        int rows = retryOnOptimisticLock(goodsId, () -> {
            GoodsInventory inv = goodsInventoryMapper.selectByGoodsId(goodsId);
            if (inv == null) {
                throw new RRException("库存不存在");
            }
            if (inv.getOccupyQuantity() < quantity) {
                throw new RRException("占用库存不足，无法取消占用");
            }
            return goodsInventoryMapper.cancelOccupy(goodsId, quantity, inv.getVersion());
        });
        if (rows == 0) {
            throw new RRException("取消占用失败，已重试" + MAX_RETRY + "次");
        }

        // 记录库存事件
        recordInventoryEvent(InventoryEventParam.builder()
                .businessType("SKU_ARRANGEMENT").sourceType("GOODS").sourceId(goodsId).eventType("CANCEL")
                .changeReal(0).changeWithhold(0).changeOccupy(-quantity).changeSellable(quantity)
                .relatedOrderNo(skuId).operatorId(operatorId).tenancyId(tenancyId)
                .build());

        log.info("货物取消占用库存成功: goodsId={}, quantity={}, skuId={}", goodsId, quantity, skuId);
    }

    public void cancelSkuInventory(CancelOccupyDTO dto) {
        int quantity = dto.getQuantity();
        String skuId = dto.getSkuId();
        String operatorId = dto.getOperatorId();
        Integer tenancyId = dto.getTenancyId();

        if (quantity <= 0) {
            throw new RRException("取消占用数量必须大于0");
        }

        // 更新sku库存（乐观锁 + 重试，lambda内含完整校验）
        int rows = retryOnOptimisticLock(skuId, () -> {
            SkuInventory sku = skuInventoryMapper.selectOne(
                    new LambdaQueryWrapper<SkuInventory>().eq(SkuInventory::getSkuId, skuId)
            );
            if (sku == null) {
                throw new RRException("SKU库存不存在");
            }
            if (sku.getRealQuantity() < quantity) {
                throw new RRException("SKU库存不足，无法取消");
            }
            return skuInventoryMapper.updateSkuInventory(skuId, -quantity, sku.getVersion());
        });
        if (rows == 0) {
            throw new RRException("SKU库存取消失败，已重试" + MAX_RETRY + "次");
        }

        // 记录库存事件
        recordInventoryEvent(InventoryEventParam.builder()
                .businessType("SKU_ARRANGEMENT").sourceType("SKU").sourceId(skuId).eventType("CANCEL")
                .changeReal(-quantity).changeWithhold(0).changeOccupy(0).changeSellable(-quantity)
                .relatedOrderNo(skuId).operatorId(operatorId).tenancyId(tenancyId)
                .build());

        log.info("SKU取消库存成功: quantity={}, skuId={}", quantity, skuId);
    }

    /**
     * 确认出库（销售/组货流转）
     */
    public void confirmOutbound(ConfirmOutboundDTO dto) {
        String goodsId = dto.getGoodsId();
        int quantity = dto.getQuantity();
        String orderNo = dto.getOrderNo();
        String operatorId = dto.getOperatorId();
        Integer tenancyId = dto.getTenancyId();

        if (quantity <= 0) {
            throw new RRException("出库数量必须大于0");
        }

        // 更新库存（乐观锁 + 重试，lambda内含完整校验）
        int rows = retryOnOptimisticLock(goodsId, () -> {
            GoodsInventory inv = goodsInventoryMapper.selectByGoodsId(goodsId);
            if (inv == null) {
                throw new RRException("库存不存在");
            }
            if (inv.getOccupyQuantity() < quantity || inv.getRealQuantity() < quantity) {
                throw new RRException("库存不足，无法确认出库");
            }
            return goodsInventoryMapper.confirmOutbound(goodsId, quantity, inv.getVersion());
        });
        if (rows == 0) {
            throw new RRException("出库确认失败，已重试" + MAX_RETRY + "次");
        }

        // 记录库存事件
        recordInventoryEvent(InventoryEventParam.builder()
                .businessType("OUTBOUND").sourceType("GOODS").sourceId(goodsId).eventType("CONFIRM")
                .changeReal(-quantity).changeWithhold(0).changeOccupy(-quantity).changeSellable(0)
                .relatedOrderNo(orderNo).operatorId(operatorId).tenancyId(tenancyId)
                .build());

        log.info("货物确认出库成功: goodsId={}, quantity={}, orderNo={}", goodsId, quantity, orderNo);
    }

    /**
     * 报损出库
     */
    public void damageOutbound(DamageOutboundDTO dto) {
        String goodsId = dto.getGoodsId();
        int quantity = dto.getQuantity();
        String orderNo = dto.getOrderNo();
        String operatorId = dto.getOperatorId();
        Integer tenancyId = dto.getTenancyId();

        if (quantity <= 0) {
            throw new RRException("出库数量必须大于0");
        }

        // 更新库存（乐观锁 + 重试，lambda内含完整校验）
        int rows = retryOnOptimisticLock(goodsId, () -> {
            GoodsInventory inv = goodsInventoryMapper.selectByGoodsId(goodsId);
            if (inv == null) {
                throw new RRException("库存不存在");
            }
            if (inv.getRealQuantity() < quantity) {
                throw new RRException("实际库存不足，无法出库");
            }
            return goodsInventoryMapper.damageOutbound(goodsId, quantity, inv.getVersion());
        });
        if (rows == 0) {
            throw new RRException("报损出库失败，已重试" + MAX_RETRY + "次");
        }

        // 记录库存事件
        recordInventoryEvent(InventoryEventParam.builder()
                .businessType("OUTBOUND").sourceType("GOODS").sourceId(goodsId).eventType("DAMAGE")
                .changeReal(-quantity).changeWithhold(0).changeOccupy(0).changeSellable(-quantity)
                .relatedOrderNo(orderNo).operatorId(operatorId).tenancyId(tenancyId)
                .build());

        log.info("货物报损出库成功: goodsId={}, quantity={}, orderNo={}", goodsId, quantity, orderNo);
    }

    /**
     * 取消入库（回滚入库库存）
     * 与 damageOutbound 的SQL操作相同（减少realQuantity和sellableQuantity）
     * 但事件语义为 INBOUND + CANCEL
     */
    public void cancelInbound(CancelInboundDTO dto) {
        String goodsId = dto.getGoodsId();
        int quantity = dto.getQuantity();
        String batchNo = dto.getBatchNo();
        String operatorId = dto.getOperatorId();
        Integer tenancyId = dto.getTenancyId();

        if (quantity <= 0) {
            throw new RRException("取消入库数量必须大于0");
        }

        // 更新库存（乐观锁 + 重试，lambda内含完整校验）复用damageOutbound的SQL
        int rows = retryOnOptimisticLock(goodsId, () -> {
            GoodsInventory inv = goodsInventoryMapper.selectByGoodsId(goodsId);
            if (inv == null) {
                throw new RRException("库存不存在");
            }
            if (inv.getRealQuantity() < quantity) {
                throw new RRException("实际库存不足，无法取消入库");
            }
            return goodsInventoryMapper.damageOutbound(goodsId, quantity, inv.getVersion());
        });
        if (rows == 0) {
            throw new RRException("取消入库失败，已重试" + MAX_RETRY + "次");
        }

        // 记录库存事件（正确语义：业务类型=INBOUND，事件类型=CANCEL）
        recordInventoryEvent(InventoryEventParam.builder()
                .businessType("INBOUND").sourceType("GOODS").sourceId(goodsId).eventType("CANCEL")
                .changeReal(-quantity).changeWithhold(0).changeOccupy(0).changeSellable(-quantity)
                .relatedOrderNo(batchNo).operatorId(operatorId).tenancyId(tenancyId)
                .build());

        log.info("取消入库成功: goodsId={}, quantity={}, batchNo={}", goodsId, quantity, batchNo);
    }

    /**
     * 盘点调整货物库存
     */
    public void adjustGoodsInventory(AdjustInventoryDTO dto) {
        String goodsId = dto.getGoodsId();
        int adjustQuantity = dto.getAdjustQuantity();
        String operatorId = dto.getOperatorId();
        Integer tenancyId = dto.getTenancyId();

        // 使用乐观锁更新（+ 重试，lambda内含完整校验）
        int rows = retryOnOptimisticLock(goodsId, () -> {
            GoodsInventory inv = goodsInventoryMapper.selectByGoodsId(goodsId);
            if (inv == null) {
                throw new RRException("库存不存在");
            }
            int afterQty = inv.getRealQuantity() + adjustQuantity;
            if (afterQty < 0) {
                throw new RRException("调整后库存不能为负数");
            }
            return goodsInventoryMapper.adjustRealQuantity(goodsId, adjustQuantity, inv.getVersion());
        });
        if (rows == 0) {
            throw new RRException("库存调整失败，已重试" + MAX_RETRY + "次");
        }

        // 记录库存事件
        recordInventoryEvent(InventoryEventParam.builder()
                .businessType("STOCKTAKE").sourceType("GOODS").sourceId(goodsId).eventType("ADJUST")
                .changeReal(adjustQuantity).changeWithhold(0).changeOccupy(0).changeSellable(adjustQuantity)
                .relatedOrderNo(null).operatorId(operatorId).tenancyId(tenancyId)
                .build());

        log.info("盘点调整库存成功: goodsId={}, adjustQuantity={}", goodsId, adjustQuantity);
    }

    /**
     * 初始化SKU库存
     */
    public void initSkuInventory(String skuId, Integer tenancyId) {
        SkuInventory skuInventory = new SkuInventory().initSkuInventory(leafSnowflakeGenerator.nextIdStr(), skuId, tenancyId);
        skuInventoryMapper.insert(skuInventory);
        log.info("初始化SKU库存成功: skuId={}", skuId);
    }

    /**
     * 查询有可售库存的货物库存列表
     */
    public List<GoodsInventory> getSellableGoodsInventory() {
        LambdaQueryWrapper<GoodsInventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.gt(GoodsInventory::getSellableQuantity, 0);
        return goodsInventoryMapper.selectList(queryWrapper);
    }

    /**
     * 查询货物库存
     */
    public GoodsInventory getGoodsInventory(String goodsId) {
        return goodsInventoryMapper.selectByGoodsId(goodsId);
    }

    /**
     * 批量查询货物库存
     */
    public List<GoodsInventory> batchGetGoodsInventory(List<String> goodsIds) {
        LambdaQueryWrapper<GoodsInventory> queryWrapper = new QueryWrapper<GoodsInventory>().lambda()
                .in(GoodsInventory::getGoodsId, goodsIds);
        return goodsInventoryMapper.selectList(queryWrapper);
    }

    /**
     * 记录库存事件（幂等性保护，失败不回滚业务事务）
     * <p>
     * 策略：Redis缓存最近5分钟的幂等键 → 数据库唯一约束兜底。
     * 事件记录为审计日志性质，任何异常均被捕获，不会向上传播导致业务回滚。
     */
    private void recordInventoryEvent(InventoryEventParam param) {
        try {
            doRecordInventoryEvent(param);
        } catch (Exception e) {
            log.error("记录库存事件失败（业务操作不受影响）: businessType={}, sourceId={}, eventType={}",
                    param.getBusinessType(), param.getSourceId(), param.getEventType(), e);
        }
    }

    private void doRecordInventoryEvent(InventoryEventParam param) {
        InventoryEvent event = new InventoryEvent();
        event.setEventId(leafSnowflakeGenerator.nextIdStr());
        event.setBusinessType(param.getBusinessType());
        event.setSourceType(param.getSourceType());
        event.setSourceId(param.getSourceId());
        event.setEventType(param.getEventType());
        event.setChangeReal(param.getChangeReal());
        event.setChangeWithhold(param.getChangeWithhold());
        event.setChangeOccupy(param.getChangeOccupy());
        event.setChangeSellable(param.getChangeSellable());
        event.setRelatedOrderNo(param.getRelatedOrderNo());
        event.setOperatorId(param.getOperatorId());
        event.setTenancyId(param.getTenancyId());
        event.setCreateTime(new Date());
        event.setIdempotentKey(event.assembleIdempotentKey());

        String idempotentKey = event.getIdempotentKey();
        String redisKey = IDEMPOTENT_KEY_PREFIX + idempotentKey;

        // 1. 快速路径：Redis缓存层校验（5分钟内的重复请求直接拦截）
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey))) {
            log.warn("库存事件已存在(Redis命中)，跳过重复记录: idempotentKey={}", idempotentKey);
            return;
        }

        // 2. 慢速路径：Redis未命中（key已过期或首次写入），回落数据库校验
        InventoryEvent existing = inventoryEventMapper.selectByIdempotentKey(idempotentKey);
        if (existing != null) {
            safeSetRedis(redisKey);
            log.warn("库存事件已存在(DB命中)，跳过重复记录: idempotentKey={}", idempotentKey);
            return;
        }

        // 3. 写入数据库（DB唯一约束兜底，DuplicateKeyException不回滚业务）
        try {
            inventoryEventMapper.insert(event);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("库存事件并发写入(唯一约束命中)，跳过: idempotentKey={}", idempotentKey);
            safeSetRedis(redisKey);
            return;
        }
        safeSetRedis(redisKey);
    }

    private void safeSetRedis(String redisKey) {
        try {
            stringRedisTemplate.opsForValue().set(redisKey, "1", IDEMPOTENT_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis缓存幂等键失败（自愈：下次回落DB）: key={}", redisKey);
        }
    }

    // ==================== 乐观锁重试工具方法 ====================

    /**
     * 乐观锁重试机制
     * 当UPDATE返回0行（版本号不匹配）时，重新查询并重试，最多重试MAX_RETRY次。
     * 每次重试前随机退避10-50ms，避免高并发下多线程同步重试造成惊群。
     *
     * @param resourceId 资源ID（用于日志）
     * @param operation  包含"查询+更新"的操作，返回影响行数
     * @return 最终影响行数
     */
    private int retryOnOptimisticLock(String resourceId, java.util.function.Supplier<Integer> operation) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            int rows = operation.get();
            if (rows > 0) {
                return rows;
            }
            if (attempt < MAX_RETRY) {
                int backoffMs = 10 + ThreadLocalRandom.current().nextInt(40);
                log.warn("乐观锁冲突，正在重试({}/{}): resourceId={}, backoffMs={}", attempt, MAX_RETRY, resourceId, backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return 0;
                }
            }
        }
        return 0;
    }
}
