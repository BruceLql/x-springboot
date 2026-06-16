package com.suke.czx.modules.warehouse.goods.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 商品分类枚举
 */
@Getter
public enum ProductTypeEnum implements IEnum<String> {

    MALE("MALE", "男款"),
    FEMALE("FEMALE", "女款"),
    CHILDREN("CHILDREN", "儿童款");

    private final String code;
    private final String desc;

    ProductTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    @JsonValue
    public String getValue() {
        return this.code;
    }

    public String getDesc() {
        return desc;
    }

    public String getCode() {
        return code;
    }

    /**
     * 根据code获取枚举
     */
    public static ProductTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ProductTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据描述获取枚举（支持前端传中文）
     */
    public static ProductTypeEnum fromDesc(String desc) {
        if (desc == null) {
            return null;
        }
        for (ProductTypeEnum type : values()) {
            if (type.getDesc().equals(desc)) {
                return type;
            }
        }
        return null;
    }
}
