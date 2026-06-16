package com.suke.czx.modules.ai.interfaces.dto.query;

import com.suke.czx.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识文档分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识文档分页查询")
public class KnowledgePageQuery extends BasePageQuery {

    @Schema(description = "分类目录", example = "01-Java基础")
    private String category;

    @Schema(description = "状态", example = "ACTIVE")
    private String status;
}
