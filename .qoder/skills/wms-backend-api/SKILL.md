---
name: wms-backend-api
description: 货仓管理系统后端API规范。包含货物管理、供应商管理、入库、出库、组商品(SKU搭配)、盘点管理等模块的完整接口定义。当用户需要生成货仓管理系统的前端代码、API对接、或提到wms、warehouse、货物、库存、SKU搭配时使用此技能。
---

# 货仓管理系统 - 后端API规范

## 一、项目概述

### 技术栈
- 后端：Spring Boot 3.5.4 + MyBatis Plus 3.5.12 + MySQL 8.0
- 认证：Token认证（Header: `Authorization`）
- 响应格式：统一JSON格式

### 统一响应结构

```typescript
interface Response<T> {
  code: number;        // 0-成功，其他-失败
  msg: string;         // 提示信息
  data: T;             // 业务数据
}
```

### 分页响应结构

```typescript
interface PageResponse<T> {
  records: T[];        // 数据列表
  total: number;       // 总记录数
  size: number;        // 每页条数
  current: number;     // 当前页码
  pages: number;       // 总页数
}
```

### 分页请求基类

```typescript
interface BasePageQuery {
  page: number;        // 页码，默认1
  size: number;        // 每页条数，默认10
}
```

---

## 二、模块接口概览

### 2.1 货物管理模块 `/warehouse/goods`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/list` | GET | 货物列表（分页） |
| `/info/{goodsId}` | GET | 货物详情 |
| `/save` | POST | 新增货物 |
| `/update` | PUT | 修改货物 |
| `/delete/{goodsId}` | DELETE | 删除货物 |
| `/options` | GET | 货物下拉选项 |

### 2.2 供应商管理模块 `/warehouse/supplier`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/list` | GET | 供应商列表（分页） |
| `/info/{supplierId}` | GET | 供应商详情 |
| `/save` | POST | 新增供应商 |
| `/update` | PUT | 修改供应商 |
| `/delete/{supplierId}` | DELETE | 删除供应商 |
| `/options` | GET | 供应商下拉选项 |
| `/goods/{supplierId}` | GET | 查询供应商关联货物 |
| `/bindGoods` | POST | 绑定货物（批量） |
| `/unbindGoods` | POST | 解绑货物 |

### 2.3 入库管理模块 `/warehouse/inbound`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/list` | GET | 入库记录列表（分页） |
| `/detail/{orderId}` | GET | 入库单详情 |
| `/items/{orderId}` | GET | 入库明细列表 |
| `/batch` | POST | 批量货物入库 ⭐核心接口 |
| `/cancel/{batchNo}` | POST | 取消入库批次 |

### 2.4 组商品（SKU搭配）模块 `/warehouse/combination`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/list` | GET | SKU列表（分页） |
| `/info/{skuId}` | GET | SKU详情 |
| `/create` | POST | 创建SKU搭配 ⭐核心接口 |
| `/arrange/{skuId}` | POST | 确定布款 |
| `/cancel/{skuId}` | POST | 取消布款 |
| `/rollback/{skuId}` | POST | 布款回滚 |
| `/delete/{skuId}` | DELETE | 删除SKU |
| `/availableGoods` | GET | 查询可搭配货物 |

### 2.5 出库管理模块 `/warehouse/outbound`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/list` | GET | 出库记录列表（分页） |
| `/damage` | POST | 报损出库 |

### 2.6 盘点管理模块 `/warehouse/stocktake`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/list` | GET | 盘点记录列表（分页） |
| `/adjustGoods` | POST | 货物库存盘点调整 |
| `/rollback/{recordId}` | POST | 盘点回滚 |

---

## 三、状态枚举

### 商品分类 ProductType
| 值 | 描述 |
|----|------|
| MALE | 男款 |
| FEMALE | 女款 |
| CHILDREN | 儿童款 |

### 供应商类型 SupplierType
| 值 | 描述 |
|----|------|
| FACTORY | 源头厂家 |
| DEALER | 二道贩子 |

### SKU状态 SkuStatus
| 值 | 描述 | 允许操作 |
|----|------|----------|
| 0 | 已取消 | 无 |
| 1 | 待布款 | 确定布款、取消布款、删除 |
| 2 | 已布款 | 布款回滚 |
| 3 | 布款回滚 | 无 |

### 出库类型 OutboundType
| 值 | 描述 |
|----|------|
| DAMAGE | 报损 |
| COMBINATION | 组货流转 |
| SALE | 销售 |

### 库存类型 StockType
| 值 | 描述 |
|----|------|
| GOODS | 货物 |
| SKU | 规格SKU |

---

## 四、核心业务流程

### 入库流程
```
1. 选择供应商 → 调用 /supplier/goods/{supplierId} 获取关联货物
2. 选择货物 → 填写数量、单价
3. 可继续添加多条货物
4. 提交 → 调用 /inbound/batch → 库存自动增加
```

### 组商品流程
```
1. 调用 /combination/availableGoods 获取有库存的货物
2. 选择货物 → 填写搭配名称、预分配数量
3. 创建 → 调用 /combination/create
   - 状态变为：待布款
   - 锁定库存(withhold_quantity)增加
4. 确定布款 → 调用 /combination/arrange/{skuId}
   - 状态变为：已布款
   - 锁定库存转占用库存(occupy_quantity)
5. 或 取消布款 → 调用 /combination/cancel/{skuId}
   - 状态变为：已取消
   - 释放锁定库存
6. 或 布款回滚 → 调用 /combination/rollback/{skuId}
   - 状态变为：已取消
   - 占用库存恢复
```

### 出库流程（报损）
```
1. 选择货物 → 填写出库数量、原因
2. 提交 → 调用 /outbound/damage
3. 直接扣减库存
```

### 盘点流程
```
1. 查询货物列表 → 点击盘点
2. 填写调整数量（正负）、原因
3. 提交 → 调用 /stocktake/adjustGoods
4. 更新库存 → 生成盘点记录
5. 可回滚 → 调用 /stocktake/rollback/{recordId}
   - 只能回滚一次
   - 库存恢复到盘点前状态
```

---

## 五、页面清单

| 页面 | 路由 | 说明 |
|------|------|------|
| 货物列表 | /warehouse/goods | 分页列表 + 新增/编辑弹窗 |
| 供应商列表 | /warehouse/supplier | 分页列表 + 详情弹窗（显示关联货物） |
| 入库管理 | /warehouse/inbound | 列表 + 批量入库弹窗 |
| 组商品 | /warehouse/combination | 列表 + 创建弹窗 + 状态操作按钮 |
| 出库管理 | /warehouse/outbound | 列表 + 报损出库弹窗 |
| 盘点管理 | /warehouse/stocktake | 列表 + 盘点调整弹窗 |

---

## 六、详细API定义

完整的数据类型定义请参考 [api-reference.md](api-reference.md)
