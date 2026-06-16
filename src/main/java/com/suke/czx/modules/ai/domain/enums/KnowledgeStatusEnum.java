package com.suke.czx.modules.ai.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 知识文档状态枚举
 */
@Getter
@AllArgsConstructor
public enum KnowledgeStatusEnum implements IEnum<String> {

    ACTIVE("ACTIVE", "正常"),
    DELETED("DELETED", "已删除");

    @EnumValue
    @JsonValue
    private final String value;

    private final String desc;
}
