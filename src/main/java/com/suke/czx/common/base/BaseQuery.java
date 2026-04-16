package com.suke.czx.common.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 基础DTO
 * 所有DTO都应继承此类
 *
 * @author lql
 * @email
 */
@Data
@Schema(description = "DTO基础类")
public class BaseQuery implements Serializable {
    private static final long serialVersionUID = 1L;

}
