-- =============================================
-- 货仓管理系统 - 完整建表SQL
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 一、基础数据模块
-- =============================================

-- ---------------------------------------------
-- 1. 货物基础信息表
-- ---------------------------------------------
DROP TABLE IF EXISTS `wms_goods`;
CREATE TABLE `wms_goods` (
  `goods_id` VARCHAR(64) NOT NULL COMMENT '货物ID',
  `goods_name` VARCHAR(100) NOT NULL COMMENT '公仔名称',
  `goods_image` VARCHAR(500) DEFAULT NULL COMMENT '公仔图片URL',
  `height` DECIMAL(10,2) DEFAULT NULL COMMENT '高度(cm)',
  `weight` DECIMAL(10,2) DEFAULT NULL COMMENT '重量(g)',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '类目',
  `product_type` VARCHAR(20) DEFAULT NULL COMMENT '商品分类: MALE-男款, FEMALE-女款, CHILDREN-儿童款',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`goods_id`),
  KEY `idx_category` (`category`),
  KEY `idx_product_type` (`product_type`),
  KEY `idx_goods_name` (`goods_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货物基础信息表';

-- ---------------------------------------------
-- 2. 供应商信息表
-- ---------------------------------------------
DROP TABLE IF EXISTS `wms_supplier`;
CREATE TABLE `wms_supplier` (
  `supplier_id` VARCHAR(64) NOT NULL COMMENT '供应商ID',
  `supplier_name` VARCHAR(100) NOT NULL COMMENT '供应商名称',
  `address` VARCHAR(200) DEFAULT NULL COMMENT '地址',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系方式',
  `supplier_type` VARCHAR(20) NOT NULL COMMENT '类型: FACTORY-源头厂家, DEALER-二道贩子',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`supplier_id`),
  KEY `idx_supplier_name` (`supplier_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商信息表';

-- ---------------------------------------------
-- 3. 供应商货物关联表 (多对多)
-- ---------------------------------------------
DROP TABLE IF EXISTS `wms_supplier_goods_rel`;
CREATE TABLE `wms_supplier_goods_rel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_id` VARCHAR(64) NOT NULL COMMENT '供应商ID',
  `goods_id` VARCHAR(64) NOT NULL COMMENT '货物ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_goods` (`supplier_id`, `goods_id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商货物关联表';

-- =============================================
-- 二、入库管理模块
-- =============================================

-- ---------------------------------------------
-- 4. 入库记录主表
-- ---------------------------------------------
DROP TABLE IF EXISTS `wms_inbound_order`;
CREATE TABLE `wms_inbound_order` (
  `order_id` VARCHAR(64) NOT NULL COMMENT '入库单ID',
  `batch_no` VARCHAR(64) NOT NULL COMMENT '批次号',
  `supplier_id` VARCHAR(64) NOT NULL COMMENT '供应商ID',
  `total_quantity` INT NOT NULL DEFAULT 0 COMMENT '总数量',
  `total_cost` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '总成本',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-取消, 1-已完成',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  KEY `idx_batch_no` (`batch_no`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库记录主表';

-- ---------------------------------------------
-- 5. 入库记录明细表
-- ---------------------------------------------
DROP TABLE IF EXISTS `wms_inbound_item`;
CREATE TABLE `wms_inbound_item` (
  `item_id` VARCHAR(64) NOT NULL COMMENT '明细ID',
  `order_id` VARCHAR(64) NOT NULL COMMENT '入库单ID',
  `batch_no` VARCHAR(64) NOT NULL COMMENT '批次号',
  `goods_id` VARCHAR(64) NOT NULL COMMENT '货物ID',
  `quantity` INT NOT NULL COMMENT '入库数量',
  `unit_cost` DECIMAL(10,2) NOT NULL COMMENT '单个成本(元)',
  `total_cost` DECIMAL(12,2) NOT NULL COMMENT '总成本',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`item_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_batch_no` (`batch_no`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库记录明细表';

-- =============================================
-- 三、库存管理模块（核心）
-- =============================================

-- ---------------------------------------------
-- 6. 货物库存表
-- ---------------------------------------------
DROP TABLE IF EXISTS `wms_goods_inventory`;
CREATE TABLE `wms_goods_inventory` (
  `inventory_id` VARCHAR(64) NOT NULL COMMENT '库存ID',
  `goods_id` VARCHAR(64) NOT NULL COMMENT '货物ID',
  `real_quantity` INT NOT NULL DEFAULT 0 COMMENT '实际库存',
  `withhold_quantity` INT NOT NULL DEFAULT 0 COMMENT '锁定库存(搭配占用)',
  `occupy_quantity` INT NOT NULL DEFAULT 0 COMMENT '占用库存(布款完成)',
  `sellable_quantity` INT NOT NULL DEFAULT 0 COMMENT '可售库存 = real - withhold - occupy',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号(乐观锁)',
  `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`inventory_id`),
  UNIQUE KEY `uk_goods_id` (`goods_id`),
  KEY `idx_sellable` (`sellable_quantity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货物库存表';

-- ---------------------------------------------
-- 7. 商品搭配表
-- ---------------------------------------------
DROP TABLE IF EXISTS `wms_sku_definition`;
CREATE TABLE `wms_sku_definition` (
  `sku_id` VARCHAR(64) NOT NULL COMMENT 'SKU ID',
  `sku_name` VARCHAR(100) NOT NULL COMMENT 'SKU名称(搭配名称)',
  `sku_image` VARCHAR(500) DEFAULT NULL COMMENT 'SKU图片',
  `pre_allocate_quantity` INT DEFAULT 0 COMMENT '预分配数量',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-已取消, 1-待布款, 2-已布款, 3-布款回滚',
  `goods_ids` VARCHAR(1024) NOT NULL COMMENT '货物ID集合，逗号拼接',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
  `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`sku_id`),
  KEY `idx_status` (`status`),
  KEY `idx_sku_name` (`sku_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规格(SKU)定义表';

-- ---------------------------------------------
-- 9. SKU库存表
-- ---------------------------------------------
DROP TABLE IF EXISTS `wms_sku_inventory`;
CREATE TABLE `wms_sku_inventory` (
  `inventory_id` VARCHAR(64) NOT NULL COMMENT '库存ID',
  `sku_id` VARCHAR(64) NOT NULL COMMENT 'SKU ID',
  `real_quantity` INT NOT NULL DEFAULT 0 COMMENT '实际库存',
  `withhold_quantity` INT NOT NULL DEFAULT 0 COMMENT '锁定库存',
  `occupy_quantity` INT NOT NULL DEFAULT 0 COMMENT '占用库存',
  `sellable_quantity` INT NOT NULL DEFAULT 0 COMMENT '可售库存',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号(乐观锁)',
  `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`inventory_id`),
  UNIQUE KEY `uk_sku_id` (`sku_id`),
  KEY `idx_sellable` (`sellable_quantity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU库存表';

-- =============================================
-- 四、出库管理模块
-- =============================================

-- ---------------------------------------------
-- 10. sku出库记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS `wms_outbound_sku_record`;
CREATE TABLE `wms_outbound_sku_record` (
  `order_id` VARCHAR(64) NOT NULL COMMENT '出库单ID',
  `order_type` VARCHAR(20) NOT NULL COMMENT '出库类型: DAMAGE-报损, COMBINATION-组货流转, SALE-销售',
  `source_type` VARCHAR(20) NOT NULL COMMENT '来源类型: GOODS-货物, SKU-规格',
  `source_id` VARCHAR(64) NOT NULL COMMENT '来源ID(货物ID或SKU ID)',
  `quantity` INT NOT NULL COMMENT '出库数量',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '出库原因',
  `related_order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联单号(如SKU订单号)',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-取消, 1-已完成',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
  `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  KEY `idx_source` (`source_type`, `source_id`),
  KEY `idx_order_type` (`order_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sku出库记录表';

-- ---------------------------------------------
-- 11. 货物出库记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS `wms_outbound_goods_record`;
CREATE TABLE `wms_outbound_goods_record` (
                                           `order_id` VARCHAR(64) NOT NULL COMMENT '出库单ID',
                                           `order_type` VARCHAR(20) NOT NULL COMMENT '出库类型: DAMAGE-报损, COMBINATION-组货流转',
                                           `source_type` VARCHAR(20) NOT NULL COMMENT '来源类型: GOODS-货物',
                                           `source_id` VARCHAR(64) NOT NULL COMMENT '货物ID)',
                                           `quantity` INT NOT NULL COMMENT '出库数量',
                                           `reason` VARCHAR(500) DEFAULT NULL COMMENT '出库原因',
                                           `related_order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联单号(如SKU订单号)',
                                           `status` TINYINT DEFAULT 1 COMMENT '状态: 0-取消, 1-已完成',
                                           `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
                                           `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
                                           `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           PRIMARY KEY (`order_id`),
                                           KEY `idx_source` (`source_type`, `source_id`),
                                           KEY `idx_order_type` (`order_type`),
                                           KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sku出库记录表';



-- =============================================
-- 五、盘点管理模块
-- =============================================

-- ---------------------------------------------
-- 11. 盘点记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS `wms_stocktake_record`;
CREATE TABLE `wms_stocktake_record` (
  `record_id` VARCHAR(64) NOT NULL COMMENT '盘点记录ID',
  `stock_type` VARCHAR(20) NOT NULL COMMENT '库存类型: GOODS-货物, SKU-规格',
  `source_id` VARCHAR(64) NOT NULL COMMENT '来源ID(货物ID或SKU ID)',
  `before_quantity` INT NOT NULL COMMENT '盘点前数量',
  `adjust_quantity` INT NOT NULL COMMENT '调整数量(正数增加,负数减少)',
  `after_quantity` INT NOT NULL COMMENT '盘点后数量',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '盘点原因',
  `is_rollback` TINYINT DEFAULT 0 COMMENT '是否已回滚: 0-否, 1-是',
  `rollback_time` DATETIME DEFAULT NULL COMMENT '回滚时间',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
  `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_source` (`stock_type`, `source_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点记录表';

-- =============================================
-- 六、库存事件流水（审计追踪）
-- =============================================

-- ---------------------------------------------
-- 12. 库存事件流水表（优化版）
-- ---------------------------------------------
DROP TABLE IF EXISTS `wms_inventory_event`;
CREATE TABLE `wms_inventory_event` (
                                       `event_id` VARCHAR(64) NOT NULL COMMENT '事件ID',
                                       `idempotent_key` VARCHAR(128) NOT NULL COMMENT '幂等键(确保请求唯一性)',
                                       `business_type` VARCHAR(20) NOT NULL COMMENT '业务类型: SKU_COMBINATION-搭配, SKU_ARRANGEMENT-布款, OUTBOUND-出库, CANCEL-取消',
                                       `source_type` VARCHAR(20) NOT NULL COMMENT '来源类型: GOODS-货物, SKU-规格',
                                       `source_id` VARCHAR(64) NOT NULL COMMENT '来源ID',
                                       `event_type` VARCHAR(20) NOT NULL COMMENT '事件类型: WITHHOLD-锁定, OCCUPY-占用, CONFIRM-确认, CANCEL-取消',
                                       `change_real` INT DEFAULT 0 COMMENT '实际库存变化',
                                       `change_withhold` INT DEFAULT 0 COMMENT '锁定库存变化',
                                       `change_occupy` INT DEFAULT 0 COMMENT '占用库存变化',
                                       `change_sellable` INT DEFAULT 0 COMMENT '可售库存变化',
                                       `related_order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联单号',
                                       `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
                                       `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待处理, 1-处理中, 2-已完成, 3-失败',
                                       `retry_count` INT DEFAULT 0 COMMENT '重试次数',
                                       `expire_time` DATETIME NOT NULL COMMENT '过期时间(默认5分钟)',
                                       `lock_key` VARCHAR(128) DEFAULT NULL COMMENT '分布式锁Key',
                                       `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
                                       `tenancy_id` INT DEFAULT NULL COMMENT '租户ID',
                                       `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       PRIMARY KEY (`event_id`),
                                       UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
                                       KEY `idx_status_expire` (`status`, `expire_time`),
                                       KEY `idx_source` (`source_type`, `source_id`),
                                       KEY `idx_business_type` (`business_type`),
                                       KEY `idx_event_type` (`event_type`),
                                       KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存事件流水表';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 初始化示例数据（可选）
-- =============================================

-- 插入示例货物
INSERT INTO `wms_goods` (`goods_id`, `goods_name`, `goods_image`, `height`, `weight`, `category`, `product_type`, `status`) VALUES
('goods001', '泰迪熊公仔', 'https://example.com/teddy.jpg', 30.00, 500.00, '毛绒玩具', 'CHILDREN', 1),
('goods002', '卡通兔子', 'https://example.com/rabbit.jpg', 25.00, 400.00, '毛绒玩具', 'FEMALE', 1),
('goods003', '超级英雄玩偶', 'https://example.com/hero.jpg', 35.00, 600.00, '动作玩具', 'MALE', 1);

-- 插入示例供应商
INSERT INTO `wms_supplier` (`supplier_id`, `supplier_name`, `address`, `contact_phone`, `supplier_type`, `status`) VALUES
('supplier001', '广州玩具厂', '广州市白云区XX路123号', '13800138001', 'FACTORY', 1),
('supplier002', '义乌商贸城', '义乌市XX街道456号', '13800138002', 'DEALER', 1);

-- 插入供应商货物关联
INSERT INTO `wms_supplier_goods_rel` (`supplier_id`, `goods_id`) VALUES
('supplier001', 'goods001'),
('supplier001', 'goods002'),
('supplier002', 'goods003');
