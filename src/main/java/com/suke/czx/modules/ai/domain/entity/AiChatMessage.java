package com.suke.czx.modules.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_chat_message")
public class AiChatMessage implements Serializable {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 会话ID */
    @TableField("session_id")
    private String sessionId;

    /** 角色：USER/ASSISTANT/SYSTEM */
    private String role;

    /** 消息内容 */
    private String content;

    /** 引用来源 JSON: [{"title":"", "filePath":"", "score":0.95}] */
    @TableField("sources")
    private String sources;

    /** Token 消耗数 */
    @TableField("token_count")
    private Integer tokenCount;

    /** 扩展元数据 JSON */
    @TableField("metadata")
    private String metadata;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;
}
