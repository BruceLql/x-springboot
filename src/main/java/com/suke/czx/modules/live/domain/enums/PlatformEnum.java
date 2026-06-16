package com.suke.czx.modules.live.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 平台枚举
 */
@Getter
@AllArgsConstructor
public enum PlatformEnum {

    DOUYIN("DOUYIN", "抖音"),
    TIKTOK("TIKTOK", "TikTok");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
