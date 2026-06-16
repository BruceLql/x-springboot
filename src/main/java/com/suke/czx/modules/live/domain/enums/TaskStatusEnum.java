package com.suke.czx.modules.live.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
@AllArgsConstructor
public enum TaskStatusEnum {

    PENDING("PENDING", "待监控"),
    MONITORING("MONITORING", "监控中"),
    STOPPED("STOPPED", "已停止"),
    INTERRUPTED("INTERRUPTED", "异常中断"),
    EXPIRED("EXPIRED", "已过期");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
