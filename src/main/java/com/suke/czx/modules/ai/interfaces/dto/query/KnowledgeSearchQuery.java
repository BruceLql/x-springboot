package com.suke.czx.modules.ai.interfaces.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 知识库语义搜索请求
 */
@Data
@Schema(description = "语义搜索请求")
public class KnowledgeSearchQuery implements Serializable {

    @NotBlank(message = "搜索关键词不能为空")
    @Schema(description = "搜索关键词", example = "HashMap 底层原理")
    private String query;

    @Schema(description = "返回结果数量", example = "5")
    private int topK = 5;

    @Schema(description = "分类筛选（可选）", example = "01-Java基础")
    private String category;
}
