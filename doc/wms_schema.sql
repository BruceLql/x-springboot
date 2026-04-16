-- ========================================================
-- 货仓管理系统数据库表结构
-- ========================================================

-- 1. 统一清理旧表（按外键依赖关系逆序删除）
DROP TABLE IF EXISTS `wms_inventory_event`;
DROP TABLE IF EXISTS `wms_stocktake_record`;
DROP TABLE IF EXISTS `wms_outbound_sku_record`;
DROP TABLE IF EXISTS `wms_outbound_goods_record`;
DROP TABLE IF EXISTS `wms_sku_inventory`;
DROP TABLE IF EXISTS `wms_sku_goods_rel`;
DROP TABLE IF EXISTS `wms_sku_definition`;
DROP TABLE IF EXISTS `wms_inbound_item`;
DROP TABLE IF EXISTS `wms_inbound_order`;
DROP TABLE IF EXISTS `wms_goods_inventory`;
DROP TABLE IF EXISTS `wms_supplier_goods_rel`;
DROP TABLE IF EXISTS `wms_supplier`;
DROP TABLE IF EXISTS `wms_goods`;

-- 2. 货物表
CREATE TABLE IF NOT EXISTS `wms_goods` (
    `goods_id` VARCHAR(64) NOT NULL COMMENT '货物ID',
    `goods_name` VARCHAR(200) NOT NULL COMMENT '公仔名称',
    `goods_image` VARCHAR(500) DEFAULT NULL COMMENT '公仔图片URL',
    `height` DECIMAL(10,2) DEFAULT NULL COMMENT '高度(cm)',
    `weight` DECIMAL(10,2) DEFAULT NULL COMMENT '重量(g)',
    `category` VARCHAR(100) DEFAULT NULL COMMENT '类目',
    `product_type` VARCHAR(20) NOT NULL COMMENT '商品分类: MALE-男款, FEMALE-女款, CHILDREN-儿童款',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`goods_id`),
    KEY `idx_goods_name` (`goods_name`),
    KEY `idx_category` (`category`),
    KEY `idx_product_type` (`product_type`),
    KEY `idx_status` (`status`),
    KEY `idx_tenancy_id` (`tenancy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货物信息表';

-- 3. 供应商表
CREATE TABLE IF NOT EXISTS `wms_supplier` (
    `supplier_id` VARCHAR(64) NOT NULL COMMENT '供应商ID',
    `supplier_name` VARCHAR(200) NOT NULL COMMENT '供应商名称',
    `address` VARCHAR(500) DEFAULT NULL COMMENT '地址',
    `contact_phone` VARCHAR(50) DEFAULT NULL COMMENT '联系方式',
    `supplier_type` VARCHAR(20) NOT NULL COMMENT '类型: FACTORY-源头厂家, DEALER-二道贩子',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`supplier_id`),
    KEY `idx_supplier_name` (`supplier_name`),
    KEY `idx_supplier_type` (`supplier_type`),
    KEY `idx_status` (`status`),
    KEY `idx_tenancy_id` (`tenancy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商信息表';

-- 4. 供应商货物关联表
CREATE TABLE IF NOT EXISTS `wms_supplier_goods_rel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `supplier_id` VARCHAR(64) NOT NULL COMMENT '供应商ID',
    `goods_id` VARCHAR(64) NOT NULL COMMENT '货物ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_supplier_goods` (`supplier_id`, `goods_id`),
    KEY `idx_supplier_id` (`supplier_id`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商货物关联表';

-- 5. 货物库存表
CREATE TABLE IF NOT EXISTS `wms_goods_inventory` (
    `inventory_id` VARCHAR(64) NOT NULL COMMENT '库存ID',
    `goods_id` VARCHAR(64) NOT NULL COMMENT '货物ID',
    `real_quantity` INT DEFAULT 0 COMMENT '实际库存',
    `withhold_quantity` INT DEFAULT 0 COMMENT '锁定库存(搭配占用)',
    `occupy_quantity` INT DEFAULT 0 COMMENT '占用库存(布款完成)',
    `sellable_quantity` INT DEFAULT 0 COMMENT '可售库存 = real - withhold - occupy',
    `version` INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`inventory_id`),
    UNIQUE KEY `uk_goods_id` (`goods_id`),
    KEY `idx_tenancy_id` (`tenancy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货物库存表';

-- 6. 入库单主表
CREATE TABLE IF NOT EXISTS `wms_inbound_order` (
    `order_id` VARCHAR(64) NOT NULL COMMENT '入库单ID',
    `batch_no` VARCHAR(64) NOT NULL COMMENT '批次号',
    `supplier_id` VARCHAR(64) NOT NULL COMMENT '供应商ID',
    `total_quantity` INT DEFAULT 0 COMMENT '总数量',
    `total_cost` DECIMAL(18,2) DEFAULT 0.00 COMMENT '总成本',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态: 0-取消, 1-已完成',
    `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`order_id`),
    KEY `idx_batch_no` (`batch_no`),
    KEY `idx_supplier_id` (`supplier_id`),
    KEY `idx_status` (`status`),
    KEY `idx_tenancy_id` (`tenancy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单主表';

-- 7. 入库明细表
CREATE TABLE IF NOT EXISTS `wms_inbound_item` (
    `item_id` VARCHAR(64) NOT NULL COMMENT '明细ID',
    `order_id` VARCHAR(64) NOT NULL COMMENT '入库单ID',
    `batch_no` VARCHAR(64) NOT NULL COMMENT '批次号',
    `goods_id` VARCHAR(64) NOT NULL COMMENT '货物ID',
    `quantity` INT NOT NULL COMMENT '入库数量',
    `unit_cost` DECIMAL(18,2) NOT NULL COMMENT '单个成本(元)',
    `total_cost` DECIMAL(18,2) NOT NULL COMMENT '总成本',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`item_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_batch_no` (`batch_no`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库明细表';

-- 8. SKU定义表（组商品）
CREATE TABLE IF NOT EXISTS `wms_sku_definition` (
    `sku_id` VARCHAR(64) NOT NULL COMMENT 'SKU ID',
    `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称(搭配名称)',
    `sku_image` VARCHAR(500) DEFAULT NULL COMMENT 'SKU图片',
    `pre_allocate_quantity` INT DEFAULT 0 COMMENT '预分配数量',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态: 0-已取消, 1-待布款, 2-已布款, 3-布款回滚',
    `goods_ids` VARCHAR(500) DEFAULT NULL COMMENT '货物ID集合，逗号拼接',
    `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`sku_id`),
    KEY `idx_status` (`status`),
    KEY `idx_tenancy_id` (`tenancy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU定义表（组商品）';

-- 9. SKU货物组成明细表
CREATE TABLE IF NOT EXISTS `wms_sku_goods_rel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sku_id` VARCHAR(64) NOT NULL COMMENT 'SKU ID',
    `goods_id` VARCHAR(64) NOT NULL COMMENT '货物ID',
    `quantity` INT DEFAULT 1 COMMENT '该货物在SKU中的数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_goods` (`sku_id`, `goods_id`),
    KEY `idx_sku_id` (`sku_id`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU货物组成明细表';

-- 10. SKU库存表
CREATE TABLE IF NOT EXISTS `wms_sku_inventory` (
    `inventory_id` VARCHAR(64) NOT NULL COMMENT '库存ID',
    `sku_id` VARCHAR(64) NOT NULL COMMENT 'SKU ID',
    `real_quantity` INT DEFAULT 0 COMMENT '实际库存',
    `withhold_quantity` INT DEFAULT 0 COMMENT '锁定库存',
    `occupy_quantity` INT DEFAULT 0 COMMENT '占用库存',
    `sellable_quantity` INT DEFAULT 0 COMMENT '可售库存',
    `version` INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`inventory_id`),
    UNIQUE KEY `uk_sku_id` (`sku_id`),
    KEY `idx_tenancy_id` (`tenancy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU库存表';

-- 11. 货物出库记录表
CREATE TABLE IF NOT EXISTS `wms_outbound_goods_record` (
    `order_id` VARCHAR(64) NOT NULL COMMENT '出库单ID',
    `order_type` VARCHAR(20) NOT NULL COMMENT '出库类型: DAMAGE-报损, COMBINATION-组货流转',
    `source_type` VARCHAR(20) DEFAULT 'GOODS' COMMENT '来源类型: GOODS-货物',
    `source_id` VARCHAR(64) NOT NULL COMMENT '货物ID',
    `quantity` INT NOT NULL COMMENT '出库数量',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '出库原因',
    `related_order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联单号(如SKU订单号)',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态: 0-取消, 1-已完成',
    `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`order_id`),
    KEY `idx_order_type` (`order_type`),
    KEY `idx_source_id` (`source_id`),
    KEY `idx_related_order_no` (`related_order_no`),
    KEY `idx_tenancy_id` (`tenancy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货物出库记录表';

-- 12. SKU出库记录表
CREATE TABLE IF NOT EXISTS `wms_outbound_sku_record` (
    `order_id` VARCHAR(64) NOT NULL COMMENT '出库单ID',
    `order_type` VARCHAR(20) NOT NULL COMMENT '出库类型: SALE-销售',
    `sku_id` VARCHAR(64) NOT NULL COMMENT 'SKU ID',
    `quantity` INT NOT NULL COMMENT '出库数量',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '出库原因',
    `related_order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联单号',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态: 0-取消, 1-已完成',
    `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`order_id`),
    KEY `idx_order_type` (`order_type`),
    KEY `idx_sku_id` (`sku_id`),
    KEY `idx_tenancy_id` (`tenancy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU出库记录表';

-- 13. 盘点记录表
CREATE TABLE IF NOT EXISTS `wms_stocktake_record` (
    `record_id` VARCHAR(64) NOT NULL COMMENT '盘点记录ID',
    `stock_type` VARCHAR(20) NOT NULL COMMENT '库存类型: GOODS-货物, SKU-规格',
    `source_id` VARCHAR(64) NOT NULL COMMENT '来源ID(货物ID或SKU ID)',
    `before_quantity` INT DEFAULT 0 COMMENT '盘点前数量',
    `adjust_quantity` INT DEFAULT 0 COMMENT '调整数量(正数增加,负数减少)',
    `after_quantity` INT DEFAULT 0 COMMENT '盘点后数量',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '盘点原因',
    `is_rollback` TINYINT(1) DEFAULT 0 COMMENT '是否已回滚: 0-否, 1-是',
    `rollback_time` DATETIME DEFAULT NULL COMMENT '回滚时间',
    `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`record_id`),
    KEY `idx_stock_type` (`stock_type`),
    KEY `idx_source_id` (`source_id`),
    KEY `idx_is_rollback` (`is_rollback`),
    KEY `idx_tenancy_id` (`tenancy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点记录表';

-- 14. 库存事件流水表
CREATE TABLE IF NOT EXISTS `wms_inventory_event` (
    `event_id` VARCHAR(64) NOT NULL COMMENT '事件ID',
    `idempotent_key` VARCHAR(128) NOT NULL COMMENT '幂等键(确保请求唯一性)',
    `business_type` VARCHAR(50) NOT NULL COMMENT '业务类型: INBOUND-入库, OUTBOUND-出库, SKU_COMBINATION-搭配, SKU_ARRANGEMENT-布款',
    `source_type` VARCHAR(20) NOT NULL COMMENT '来源类型: GOODS-货物, SKU-规格',
    `source_id` VARCHAR(64) NOT NULL COMMENT '来源ID',
    `event_type` VARCHAR(20) NOT NULL COMMENT '事件类型: WITHHOLD-锁定, OCCUPY-占用, CONFIRM-确认, CANCEL-取消',
    `change_real` INT DEFAULT 0 COMMENT '实际库存变化',
    `change_withhold` INT DEFAULT 0 COMMENT '锁定库存变化',
    `change_occupy` INT DEFAULT 0 COMMENT '占用库存变化',
    `change_sellable` INT DEFAULT 0 COMMENT '可售库存变化',
    `related_order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联单号',
    `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`event_id`),
    UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
    KEY `idx_business_type` (`business_type`),
    KEY `idx_source_type_id` (`source_type`, `source_id`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_related_order_no` (`related_order_no`),
    KEY `idx_tenancy_id` (`tenancy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存事件流水表';
