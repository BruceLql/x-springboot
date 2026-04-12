package com.suke.czx.modules.shortLink.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 短链生成请求
 *
 * @author 李启岚
 * @Date 2026/4/11
 */
@Data
@Getter
@Setter
public class ShortLinkRequest implements Serializable {
    public static final long serialVersionUID = 1L;

    @Schema(description = "原始长链接", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(value = "originalUrl")
    public String originalUrl;

    @Schema(description = "租户ID")
    @JsonProperty(value = "tenancyId")
    public Long tenancyId;

    @Schema(description = "过期时间")
    @JsonProperty(value = "expireTime")
    public Date expireTime;

    @Schema(description = "备注")
    @JsonProperty(value = "remark")
    public String remark;

    @Schema(description = "是否自定义短链码")
    @JsonProperty(value = "customCode")
    public Boolean customCode;

    @Schema(description = "自定义短链码")
    @JsonProperty(value = "shortCode")
    public String shortCode;
}
