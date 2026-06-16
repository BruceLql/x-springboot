package com.suke.czx.modules.ai.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试方向枚举
 */
@Getter
@AllArgsConstructor
public enum InterviewDirectionEnum implements IEnum<String> {

    JAVA_BASIC("JAVA_BASIC", "Java基础"),
    SPRING_ECOSYSTEM("SPRING_ECOSYSTEM", "Spring生态"),
    DATABASE("DATABASE", "数据库"),
    DISTRIBUTED("DISTRIBUTED", "分布式系统"),
    SYSTEM_DESIGN("SYSTEM_DESIGN", "系统设计"),
    AI_KNOWLEDGE("AI_KNOWLEDGE", "AI知识"),
    COMPREHENSIVE("COMPREHENSIVE", "综合");

    @EnumValue
    @JsonValue
    private final String value;

    private final String desc;

    /**
     * 根据面试方向获取对应的知识库分类目录
     */
    public String getKnowledgeCategory() {
        return switch (this) {
            case JAVA_BASIC -> "01-Java基础";
            case SPRING_ECOSYSTEM -> "02-Spring生态";
            case DATABASE -> "03-数据库";
            case DISTRIBUTED -> "04-分布式系统";
            case SYSTEM_DESIGN -> "05-系统设计";
            case AI_KNOWLEDGE -> "12-AI知识";
            case COMPREHENSIVE -> null; // null = 全部
        };
    }
}
