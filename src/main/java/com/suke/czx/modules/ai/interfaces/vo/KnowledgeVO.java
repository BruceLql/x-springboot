package com.suke.czx.modules.ai.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文档 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识文档")
public class KnowledgeVO implements Serializable {

    @Schema(description = "文档ID")
    private String id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "分块数")
    private Integer chunkCount;

    @Schema(description = "总字数")
    private Integer wordCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
