package com.suke.czx.modules.ai.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试难度级别枚举
 */
@Getter
@AllArgsConstructor
public enum DifficultyLevelEnum implements IEnum<String> {

    JUNIOR("JUNIOR", "初级"),
    MIDDLE("MIDDLE", "中级"),
    SENIOR("SENIOR", "高级");

    @EnumValue
    @JsonValue
    private final String value;

    private final String desc;
}
