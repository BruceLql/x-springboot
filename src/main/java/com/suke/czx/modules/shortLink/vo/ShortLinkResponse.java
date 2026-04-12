package com.suke.czx.modules.shortLink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 短链响应
 *
 * @author 李启岚
 * @Date 2026/4/11
 */
@Data
public class ShortLinkResponse implements Serializable {
    public static final long serialVersionUID = 1L;

    @Schema(description = "短链ID")
    @JsonProperty(value = "linkId")
    @JsonSerialize(using = ToStringSerializer.class)
    public String linkId;

    @Schema(description = "原始长链接")
    @JsonProperty(value = "originalUrl")
    public String originalUrl;

    @Schema(description = "短链码")
    @JsonProperty(value = "shortCode")
    public String shortCode;

    @Schema(description = "完整短链接")
    @JsonProperty(value = "shortUrl")
    public String shortUrl;

    @Schema(description = "租户ID")
    @JsonProperty(value = "tenancyId")
    @JsonSerialize(using = ToStringSerializer.class)
    public Long tenancyId;

    @Schema(description = "租户名称")
    @JsonProperty(value = "tenancyName")
    public String tenancyName;

    @Schema(description = "访问次数")
    @JsonProperty(value = "visitCount")
    public Integer visitCount;

    @Schema(description = "状态 0-禁用 1-启用")
    @JsonProperty(value = "status")
    public Integer status;

    @Schema(description = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty(value = "expireTime")
    public Date expireTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty(value = "createTime")
    public Date createTime;

    @Schema(description = "备注")
    @JsonProperty(value = "remark")
    public String remark;
}
