package com.suke.czx.modules.warehouse.inventory.domain.enums;

import com.suke.czx.common.utils.Constant;
import lombok.Getter;

/**
 * 库存分布式锁Key枚举
 */
@Getter
public enum InventoryLockKey {

    GOODS_INVENTORY("inventory:goods:"),
    SKU_INVENTORY("inventory:sku:");

    private final String prefix;

    InventoryLockKey(String prefix) {
        this.prefix = prefix;
    }

    /**
     * 传入业务ID，返回完整的锁名称
     */
    public String format(String id) {
        return Constant.SYSTEM_NAME + ":" + prefix + id;
    }
}
