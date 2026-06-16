package com.suke.czx.modules.ai.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会话类型枚举
 */
@Getter
@AllArgsConstructor
public enum SessionTypeEnum implements IEnum<String> {

    CHAT("CHAT", "AI对话"),
    INTERVIEW("INTERVIEW", "模拟面试");

    @EnumValue
    @JsonValue
    private final String value;

    private final String desc;
}
