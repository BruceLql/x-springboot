package com.suke.czx.modules.shortLink.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 短链生成请求
 *
 * @author 李启岚
 * @Date 2026/4/11
 */
@Data
public class ShortLinkQueryByIdRequest implements Serializable {
    public static final long serialVersionUID = 1L;

    @Schema(description = "租户ID")
    @JsonProperty(value = "tenancyId")
    public Long tenancyId;

    @Schema(description = "短链ID")
    @JsonProperty(value = "linkId")
    @JsonSerialize(using = ToStringSerializer.class)
    public String linkId;


    @Schema(description = "状态 0-禁用 1-启用")
    @JsonProperty(value = "status")
    public Integer status;


}
