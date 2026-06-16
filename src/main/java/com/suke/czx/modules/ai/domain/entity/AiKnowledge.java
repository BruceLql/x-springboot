package com.suke.czx.modules.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.suke.czx.modules.ai.domain.enums.KnowledgeStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文档实体（MySQL 元数据，向量存 PGVector）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_knowledge")
public class AiKnowledge implements Serializable {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 文档标题 */
    private String title;

    /** 分类（目录名，如 01-Java基础） */
    private String category;

    /** 文件路径 */
    @TableField("file_path")
    private String filePath;

    /** 状态 */
    private KnowledgeStatusEnum status;

    /** 分块数量 */
    @TableField("chunk_count")
    private Integer chunkCount;

    /** 总字数 */
    @TableField("word_count")
    private Integer wordCount;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
