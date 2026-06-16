package com.suke.czx.modules.warehouse.inventory.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.common.lock.RedissonLock;
import com.suke.czx.modules.warehouse.inventory.domain.entity.GoodsInventory;
import com.suke.czx.modules.warehouse.inventory.domain.enums.InventoryLockKey;
import com.suke.czx.modules.warehouse.inventory.domain.service.InventoryDomainService;
import com.suke.czx.modules.warehouse.inventory.infrastructure.repository.GoodsInventoryMapper;
import com.suke.czx.modules.warehouse.inventory.infrastructure.repository.SkuInventoryMapper;
import com.suke.czx.modules.warehouse.inventory.domain.command.*;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.BatchGoodsInventoryQueryDTO;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.GoodsInventoryPageQuery;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.GoodsInventoryQueryDTO;
import com.suke.czx.modules.warehouse.inventory.interfaces.dto.query.SkuInventoryPageQuery;
import com.suke.czx.modules.warehouse.inventory.interfaces.vo.GoodsInventoryVO;
import com.suke.czx.modules.warehouse.inventory.interfaces.vo.SkuInventoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 库存应用服务 对外统一提供库存服务不去分析货物库存和SKU库存
 * 内部细分货物库存 和 SKU库存 【注意业务逻辑区分】
 *
 * 分布式锁策略：
 * 1. leaseTime=-1 启用Redisson看门狗自动续期（默认30s续期，防止业务超时锁失效）
 * 2. 锁释放注册在事务afterCompletion回调中，确保事务提交/回滚后才释放锁
 * 3. 锁粒度与操作资源匹配：操作goods_inventory使用GOODS锁，操作sku_inventory使用SKU锁
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryApplicationService implements InventoryService {

    private final GoodsInventoryMapper goodsInventoryMapper;
    private final SkuInventoryMapper skuInventoryMapper;
    private final InventoryDomainService inventoryDomainService;
    private final RedissonLock redissonLock;

    @Value("${inventory.lock.timeout:30}")
    private int lockTimeoutSeconds;

    /**
     * 分页查询货物库存列表
     */
    @Override
    public IPage<GoodsInventoryVO> queryPage(GoodsInventoryPageQuery query) {
        Page<GoodsInventoryVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<GoodsInventoryVO> result = goodsInventoryMapper.selectInventoryPage(page, query);

        // 注入计算字段
        for (GoodsInventoryVO vo : result.getRecords()) {
            vo.setProductTypeDesc(getProductTypeDescription(vo.getProductType()));
            vo.setStatusDesc(vo.getStatus() != null && vo.getStatus() == 1 ? "启用" : "禁用");
        }

        return result;
    }

    /**
     * 分页查询SKU库存列表
     */
    @Override
    public IPage<SkuInventoryVO> querySkuPage(SkuInventoryPageQuery query) {
        Page<SkuInventoryVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<SkuInventoryVO> result = skuInventoryMapper.selectSkuInventoryPage(page, query);

        // 注入计算字段
        for (SkuInventoryVO vo : result.getRecords()) {
            vo.setStatusDesc(getSkuStatusDesc(vo.getStatus()));
        }

        return result;
    }

    /**
     * 获取商品分类描述
     */
    private String getProductTypeDescription(String productType) {
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
     * 获取SKU状态描述
     */
    private String getSkuStatusDesc(Integer status) {
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

    /**
     * 初始化货物库存
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void initGoodsInventory(InitInventoryDTO dto) {
        String lockKey = InventoryLockKey.GOODS_INVENTORY.format(dto.getGoodsId());
        acquireLock(lockKey, "获取库存初始化锁失败，请重试");
        inventoryDomainService.initGoodsInventory(dto);
    }

    /**
     * 增加实际库存（入库）
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void increaseInventory(IncreaseInventoryDTO dto) {
        String lockKey = InventoryLockKey.GOODS_INVENTORY.format(dto.getGoodsId());
        acquireLock(lockKey, "获取库存增加锁失败，请重试");
        inventoryDomainService.increaseInventory(dto);
    }

    /**
     * 锁定库存（组商品）
     * 锁粒度：操作的是goods_inventory表，使用GOODS_INVENTORY锁
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void withholdInventory(WithholdInventoryDTO dto) {
        String lockKey = InventoryLockKey.GOODS_INVENTORY.format(dto.getGoodsId());
        acquireLock(lockKey, "获取库存锁定锁失败，请重试");
        inventoryDomainService.withholdInventory(dto);
    }

    /**
     * 释放锁定库存（取消组商品）
     * 锁粒度：操作的是goods_inventory表，使用GOODS_INVENTORY锁
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void releaseWithhold(ReleaseWithholdDTO dto) {
        String lockKey = InventoryLockKey.GOODS_INVENTORY.format(dto.getGoodsId());
        acquireLock(lockKey, "获取库存释放锁失败，请重试");
        inventoryDomainService.releaseWithhold(dto);
    }

    /**
     * 占用库存（布款完成）
     * 锁粒度：操作的是goods_inventory表，使用GOODS_INVENTORY锁
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void occupyInventory(OccupyInventoryDTO dto) {
        String lockKey = InventoryLockKey.GOODS_INVENTORY.format(dto.getGoodsId());
        acquireLock(lockKey, "获取库存占用锁失败，请重试");
        inventoryDomainService.occupyInventory(dto);
    }

    /**
     * 批量占用库存（布款完成）
     * 锁策略：按资源ID排序后依次获取锁，防止死锁
     * 涉及多个goods_inventory + 一个sku_inventory
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void occupyInventoryBatch(OccupyInventoryBatchDTO dto) {
        // 收集所有需要加锁的key并排序，防止死锁
        List<String> lockKeys = new ArrayList<>();
        for (String goodsId : dto.getGoodsIdList()) {
            lockKeys.add(InventoryLockKey.GOODS_INVENTORY.format(goodsId));
        }
        lockKeys.add(InventoryLockKey.SKU_INVENTORY.format(dto.getSkuId()));
        Collections.sort(lockKeys);

        // 按排序顺序依次获取锁
        acquireMultiLocks(lockKeys, "获取库存占用锁失败，请重试");

        // 批量占用库存 修改数据库
        inventoryDomainService.occupyInventoryBatch(dto);
    }

    /**
     * 取消占用库存（布款回滚）
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelOccupy(CancelOccupyDTO dto) {
        String lockKey = InventoryLockKey.GOODS_INVENTORY.format(dto.getGoodsId());
        acquireLock(lockKey, "获取库存取消占用锁失败，请重试");
        inventoryDomainService.cancelOccupy(dto);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelSkuInventory(CancelOccupyDTO dto) {
        String lockKey = InventoryLockKey.SKU_INVENTORY.format(dto.getSkuId());
        acquireLock(lockKey, "获取SKU库存取消锁失败，请重试");
        inventoryDomainService.cancelSkuInventory(dto);
    }

    /**
     * 确认出库（销售/组货流转）
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void confirmOutbound(ConfirmOutboundDTO dto) {
        String lockKey = InventoryLockKey.GOODS_INVENTORY.format(dto.getGoodsId());
        acquireLock(lockKey, "获取库存确认出库锁失败，请重试");
        inventoryDomainService.confirmOutbound(dto);
    }

    /**
     * 报损出库
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void damageOutbound(DamageOutboundDTO dto) {
        String lockKey = InventoryLockKey.GOODS_INVENTORY.format(dto.getGoodsId());
        acquireLock(lockKey, "获取库存报损出库锁失败，请重试");
        inventoryDomainService.damageOutbound(dto);
    }

    /**
     * 取消入库（回滚入库库存）
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelInbound(CancelInboundDTO dto) {
        String lockKey = InventoryLockKey.GOODS_INVENTORY.format(dto.getGoodsId());
        acquireLock(lockKey, "获取库存取消入库锁失败，请重试");
        inventoryDomainService.cancelInbound(dto);
    }

    /**
     * 盘点调整货物库存
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adjustGoodsInventory(AdjustInventoryDTO dto) {
        String lockKey = InventoryLockKey.GOODS_INVENTORY.format(dto.getGoodsId());
        acquireLock(lockKey, "获取库存调整锁失败，请重试");
        inventoryDomainService.adjustGoodsInventory(dto);
    }

    /**
     * 初始化SKU库存
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void initSkuInventory(String skuId, Integer tenancyId) {
        String lockKey = InventoryLockKey.SKU_INVENTORY.format(skuId);
        acquireLock(lockKey, "获取SKU库存初始化锁失败，请重试");
        inventoryDomainService.initSkuInventory(skuId, tenancyId);
    }

    /**
     * 查询有可售库存的货物库存列表
     */
    @Override
    public List<GoodsInventory> getSellableGoodsInventory() {
        return inventoryDomainService.getSellableGoodsInventory();
    }

    /**
     * 查询货物库存
     */
    @Override
    public GoodsInventory getGoodsInventory(GoodsInventoryQueryDTO dto) {
        return inventoryDomainService.getGoodsInventory(dto.getGoodsId());
    }

    /**
     * 批量查询货物库存
     */
    @Override
    public List<GoodsInventory> batchGetGoodsInventory(BatchGoodsInventoryQueryDTO dto) {
        return inventoryDomainService.batchGetGoodsInventory(dto.getGoodsIds());
    }

    // ==================== 锁管理方法 ====================

    /**
     * 获取分布式锁，并注册事务完成后自动释放
     * leaseTime=-1 启用看门狗自动续期，避免业务耗时超过固定租约导致锁提前失效
     * 锁释放延迟到事务提交/回滚之后，确保数据一致性
     */
    private void acquireLock(String lockKey, String failMessage) {
        boolean locked = redissonLock.lock(lockKey, lockTimeoutSeconds, -1);
        if (!locked) {
            throw new RRException(failMessage);
        }
        registerUnlockAfterCompletion(lockKey);
    }

    /**
     * 批量获取分布式锁（必须已排序），任一获取失败则释放已获取的锁并抛异常
     */
    private void acquireMultiLocks(List<String> sortedLockKeys, String failMessage) {
        List<String> acquiredKeys = new ArrayList<>();
        try {
            for (String lockKey : sortedLockKeys) {
                boolean locked = redissonLock.lock(lockKey, lockTimeoutSeconds, -1);
                if (!locked) {
                    throw new RRException(failMessage + " key=" + lockKey);
                }
                acquiredKeys.add(lockKey);
            }
            // 全部获取成功，注册事务完成后释放
            for (String lockKey : acquiredKeys) {
                registerUnlockAfterCompletion(lockKey);
            }
        } catch (Exception e) {
            // 获取失败，立即释放已获取的锁
            for (String key : acquiredKeys) {
                redissonLock.unlock(key);
            }
            throw e;
        }
    }

    /**
     * 注册事务完成后释放锁的回调
     * 无论事务提交还是回滚，都会在事务结束后释放锁
     * 当被外层事务包裹时（如CombinationApplicationService.createSku），
     * 锁会等待最外层事务完成后再释放，确保事务隔离性
     */
    private void registerUnlockAfterCompletion(String lockKey) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    redissonLock.unlock(lockKey);
                }
            });
        } else {
            // 无活跃事务同步（理论上不应出现，因为方法有@Transactional）
            log.warn("无活跃事务同步，直接释放锁: {}", lockKey);
            redissonLock.unlock(lockKey);
        }
    }
}
