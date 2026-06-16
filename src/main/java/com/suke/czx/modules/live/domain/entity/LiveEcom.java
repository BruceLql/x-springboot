package com.suke.czx.modules.live.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直播电商数据实体
 */
@Data
@TableName("live_ecom")
@Schema(description = "直播电商数据")
public class LiveEcom implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "电商数据ID")
    private String ecomId;

    @Schema(description = "任务ID")
    private String taskId;

    /**
     * 消息类型（库原始分类）
     * SHOPPING: 直播购物消息
     * ECOM: 直播电商通用消息
     * RANK: 电商排行榜消息
     * SYNC: 房间数据同步消息
     */
    @Schema(description = "消息类型: SHOPPING/ECOM/RANK/SYNC")
    private String ecomType;

    @Schema(description = "抖音消息子类型")
    private Long msgType;

    /**
     * 业务类型（从 content JSON 解析）
     * ORDER: 下单
     * PRODUCT_ON: 商品上架
     * PRODUCT_OFF: 商品下架
     * PROMOTION: 促销活动
     * CART: 加购
     * OTHER: 其他
     */
    @Schema(description = "业务类型: ORDER/PRODUCT_ON/PRODUCT_OFF/PROMOTION/CART/OTHER")
    private String businessType;

    @Schema(description = "原始 JSON 数据")
    private String rawJson;

    @Schema(description = "消息时间戳")
    private Long msgTimestamp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Date createTime;
}
