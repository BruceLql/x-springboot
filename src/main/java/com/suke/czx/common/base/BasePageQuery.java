package com.suke.czx.common.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
/**
 * 基础分页查询DTO
 * 所有分页查询条件都应继承此类
 *
 * @author lql
 * @email
 */
@Data
@Schema(description = "分页查询基类")
public class BasePageQuery extends BaseQuery {
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码", example = "1")
    private Long page = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;

    @Schema(description = "排序字段", example = "create_time")
    private String orderBy;

    @Schema(description = "排序方式：asc-升序, desc-降序", example = "desc")
    private String orderDirection = "desc";
}
