# API 详细定义

## 1. 货物管理模块

### 1.1 货物列表

**请求**
```
GET /warehouse/goods/list
```

**参数**
```typescript
interface GoodsPageQuery extends BasePageQuery {
  goodsName?: string;    // 公仔名称（模糊搜索）
  category?: string;     // 类目
  productType?: string;  // 商品分类：MALE/FEMALE/CHILDREN
  status?: number;       // 状态：0-禁用，1-启用
}
```

**响应**
```typescript
interface GoodsVO {
  goodsId: string;
  goodsName: string;
  goodsImage: string;
  height: number;           // 高度
  weight: number;           // 重量
  category: string;
  productType: string;
  productTypeDesc: string;  // 分类描述：男款/女款/儿童款
  status: number;
  statusDesc: string;       // 状态描述：启用/禁用
  createTime: string;
}
```

### 1.2 货物详情

**请求**
```
GET /warehouse/goods/info/{goodsId}
```

**响应**
```typescript
interface GoodsDetailVO extends GoodsVO {
  tenancyId: number;
  updateTime: string;
}
```

### 1.3 新增货物

**请求**
```
POST /warehouse/goods/save
Content-Type: application/json
```

**参数**
```typescript
interface GoodsCreateCommand {
  goodsName: string;       // 必填
  goodsImage?: string;
  height?: number;
  weight?: number;
  category?: string;
  productType: string;     // 必填，MALE/FEMALE/CHILDREN
}
```

### 1.4 修改货物

**请求**
```
PUT /warehouse/goods/update
Content-Type: application/json
```

**参数**
```typescript
interface GoodsUpdateCommand extends GoodsCreateCommand {
  goodsId: string;         // 必填
  status?: number;
}
```

---

## 2. 供应商管理模块

### 2.1 供应商列表

**请求**
```
GET /warehouse/supplier/list
```

**参数**
```typescript
interface SupplierPageQuery extends BasePageQuery {
  supplierName?: string;    // 供应商名称（模糊搜索）
  supplierType?: string;    // 类型：FACTORY/DEALER
  status?: number;
}
```

**响应**
```typescript
interface SupplierVO {
  supplierId: string;
  supplierName: string;
  address: string;
  contactPhone: string;
  supplierType: string;
  supplierTypeDesc: string;  // 源头厂家/二道贩子
  status: number;
  statusDesc: string;
  createTime: string;
}
```

### 2.2 供应商详情

**请求**
```
GET /warehouse/supplier/info/{supplierId}
```

**响应**
```typescript
interface SupplierDetailVO extends SupplierVO {
  tenancyId: number;
  updateTime: string;
  goodsList: SupplierGoodsVO[];  // 关联货物列表
}

interface SupplierGoodsVO {
  id: number;
  goodsId: string;
  goodsName: string;
  goodsImage: string;
  height: number;
  weight: number;
  category: string;
  productType: string;
  productTypeDesc: string;
}
```

### 2.3 新增供应商

**请求**
```
POST /warehouse/supplier/save
Content-Type: application/json
```

**参数**
```typescript
interface SupplierCreateCommand {
  supplierName: string;      // 必填
  address?: string;
  contactPhone?: string;
  supplierType: string;      // 必填，FACTORY/DEALER
}
```

### 2.4 绑定货物

**请求**
```
POST /warehouse/supplier/bindGoods
Content-Type: application/json
```

**参数**
```typescript
interface SupplierBindGoodsCommand {
  supplierId: string;        // 必填
  goodsIds: string[];        // 必填，货物ID数组
}
```

### 2.5 解绑货物

**请求**
```
POST /warehouse/supplier/unbindGoods
Content-Type: application/json
```

**参数**
```typescript
interface SupplierUnbindGoodsCommand {
  supplierId: string;        // 必填
  goodsId: string;           // 必填
}
```

---

## 3. 入库管理模块

### 3.1 入库记录列表

**请求**
```
GET /warehouse/inbound/list
```

**参数**
```typescript
interface InboundPageQuery extends BasePageQuery {
  batchNo?: string;       // 批次号
  supplierId?: string;    // 供应商ID
  status?: number;        // 0-取消，1-已完成
}
```

**响应**
```typescript
interface InboundOrder {
  orderId: string;
  batchNo: string;
  supplierId: string;
  totalQuantity: number;
  totalCost: number;
  status: number;
  operatorId: string;
  remark: string;
  tenancyId: number;
  createTime: string;
  updateTime: string;
}
```

### 3.2 入库明细列表

**请求**
```
GET /warehouse/inbound/items/{orderId}
```

**响应**
```typescript
interface InboundItemVO {
  itemId: string;
  orderId: string;
  batchNo: string;
  goodsId: string;
  goodsName: string;       // 货物名称
  goodsImage: string;      // 货物图片URL
  quantity: number;
  unitCost: number;
  totalCost: number;
  createTime: string;
}
```

### 3.3 批量货物入库 ⭐核心接口

**请求**
```
POST /warehouse/inbound/batch
Content-Type: application/json
```

**参数**
```typescript
interface BatchInboundRequest {
  supplierId: string;           // 必填，供应商ID
  items: InboundItemDTO[];      // 必填，入库明细列表
  remark?: string;              // 备注
}

interface InboundItemDTO {
  goodsId: string;              // 货物ID
  quantity: number;             // 入库数量
  unitCost: number;             // 单个成本（元）
}
```

**响应**
```typescript
// 返回 batchNo（批次号）
```

---

## 4. 组商品（SKU搭配）模块

### 4.1 SKU列表

**请求**
```
GET /warehouse/combination/list
```

**参数**
```typescript
interface SkuPageQuery extends BasePageQuery {
  skuName?: string;     // SKU名称（模糊搜索）
  status?: number;      // 0-已取消，1-待布款，2-已布款，3-布款回滚
}
```

**响应**
```typescript
interface SkuDefinition {
  skuId: string;
  skuName: string;
  skuImage: string;
  preAllocateQuantity: number;  // 预分配数量
  status: number;
  goodsIds: string;             // 货物ID集合，逗号分隔
  operatorId: string;
  tenancyId: number;
  createTime: string;
  updateTime: string;
}
```

### 4.2 创建SKU搭配 ⭐核心接口

**请求**
```
POST /warehouse/combination/create
Content-Type: application/json
```

**参数**
```typescript
interface CreateSkuRequest {
  skuName: string;              // 必填，搭配名称
  skuImage?: string;            // SKU图片
  preAllocateQuantity: number;  // 必填，预分配数量
  goodsIds: string[];           // 必填，货物ID数组
}
```

**业务说明**
- 创建时会锁定各货物的库存（withhold_quantity）
- SKU状态变为"待布款"（status=1）

### 4.3 确定布款

**请求**
```
POST /warehouse/combination/arrange/{skuId}
```

**业务说明**
- 只有"待布款"状态可执行
- 执行后锁定库存转为占用库存
- SKU状态变为"已布款"（status=2）

### 4.4 取消布款

**请求**
```
POST /warehouse/combination/cancel/{skuId}
```

**业务说明**
- 只有"待布款"状态可执行
- 释放锁定的库存
- SKU状态变为"已取消"（status=0）

### 4.5 布款回滚

**请求**
```
POST /warehouse/combination/rollback/{skuId}
```

**业务说明**
- 只有"已布款"状态可执行
- 取消占用，库存恢复
- SKU状态变为"已取消"（status=0）

---

## 5. 出库管理模块

### 5.1 出库记录列表

**请求**
```
GET /warehouse/outbound/list
```

**参数**
```typescript
interface OutboundPageQuery extends BasePageQuery {
  orderType?: string;    // DAMAGE-报损, COMBINATION-组货流转
  sourceId?: string;     // 货物ID
}
```

**响应**
```typescript
interface OutboundGoodsRecord {
  orderId: string;
  orderType: string;
  sourceType: string;    // GOODS
  sourceId: string;      // 货物ID
  quantity: number;
  reason: string;
  relatedOrderNo: string;
  status: number;
  operatorId: string;
  tenancyId: number;
  createTime: string;
  updateTime: string;
}
```

### 5.2 报损出库

**请求**
```
POST /warehouse/outbound/damage
Content-Type: application/json
```

**参数**
```typescript
interface DamageOutboundRequest {
  goodsId: string;       // 必填，货物ID
  quantity: number;      // 必填，出库数量
  reason?: string;       // 出库原因
}
```

**业务说明**
- 直接扣减货物的实际库存和可售库存
- 生成出库记录

---

## 6. 盘点管理模块

### 6.1 盘点记录列表

**请求**
```
GET /warehouse/stocktake/list
```

**参数**
```typescript
interface StocktakePageQuery extends BasePageQuery {
  stockType?: string;    // GOODS-货物, SKU-规格
}
```

**响应**
```typescript
interface StocktakeRecord {
  recordId: string;
  stockType: string;       // GOODS/SKU
  sourceId: string;
  beforeQuantity: number;  // 盘点前数量
  adjustQuantity: number;  // 调整数量（正负）
  afterQuantity: number;   // 盘点后数量
  reason: string;
  isRollback: number;      // 是否已回滚
  rollbackTime: string;
  operatorId: string;
  tenancyId: number;
  createTime: string;
  updateTime: string;
}
```

### 6.2 货物库存盘点调整

**请求**
```
POST /warehouse/stocktake/adjustGoods
Content-Type: application/json
```

**参数**
```typescript
interface AdjustGoodsRequest {
  goodsId: string;          // 必填，货物ID
  adjustQuantity: number;   // 必填，调整数量（正数增加，负数减少）
  reason?: string;          // 盘点原因
}
```

**业务说明**
- 调整货物的实际库存
- 自动计算可售库存
- 生成盘点记录

### 6.3 盘点回滚

**请求**
```
POST /warehouse/stocktake/rollback/{recordId}
```

**业务说明**
- 只能回滚一次
- 库存恢复到盘点前状态

---

## 7. 库存管理模块

### 7.1 货物库存列表

**请求**
```
GET /warehouse/inventory/goods/list
```

**参数**
```typescript
interface GoodsInventoryPageQuery extends BasePageQuery {
  goodsName?: string;     // 公仔名称（模糊搜索）
  category?: string;      // 类目
  productType?: string;   // 商品分类：MALE/FEMALE/CHILDREN
  status?: number;        // 货物状态：0-禁用，1-启用
}
```

**响应**
```typescript
interface GoodsInventoryVO {
  inventoryId: string;
  goodsId: string;
  goodsName: string;           // 公仔名称
  goodsImage: string;          // 公仔图片URL
  category: string;
  productType: string;
  productTypeDesc: string;     // 男款/女款/儿童款
  status: number;
  statusDesc: string;          // 启用/禁用
  realQuantity: number;        // 实际库存
  withholdQuantity: number;    // 锁定库存(搭配占用)
  occupyQuantity: number;      // 占用库存(布款完成)
  sellableQuantity: number;    // 可售库存
  version: number;             // 版本号(乐观锁)
  createTime: string;
}
```

### 7.2 SKU库存列表

**请求**
```
GET /warehouse/inventory/sku/list
```

**参数**
```typescript
interface SkuInventoryPageQuery extends BasePageQuery {
  skuName?: string;       // SKU名称（模糊搜索）
  status?: number;        // SKU状态：0-已取消, 1-待布款, 2-已布款, 3-布款回滚
}
```

**响应**
```typescript
interface SkuInventoryVO {
  inventoryId: string;
  skuId: string;
  skuName: string;             // SKU名称
  skuImage: string;            // SKU图片
  preAllocateQuantity: number; // 预分配数量
  status: number;
  statusDesc: string;          // 已取消/待布款/已布款/布款回滚
  realQuantity: number;        // 实际库存
  withholdQuantity: number;    // 锁定库存
  occupyQuantity: number;      // 占用库存
  sellableQuantity: number;    // 可售库存
  version: number;             // 版本号(乐观锁)
  createTime: string;
}
```
