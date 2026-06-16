package com.suke.czx.modules.ai.application.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suke.czx.modules.ai.domain.entity.AiKnowledge;
import com.suke.czx.modules.ai.domain.enums.KnowledgeStatusEnum;
import com.suke.czx.modules.ai.infrastructure.ai.VectorStoreService;
import com.suke.czx.modules.ai.infrastructure.document.MarkdownKnowledgeLoader;
import com.suke.czx.modules.ai.infrastructure.repository.AiKnowledgeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库应用服务
 * <p>
 * 编排知识库导入、管理、搜索等用例
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeAppService {

    private final AiKnowledgeMapper knowledgeMapper;
    private final MarkdownKnowledgeLoader knowledgeLoader;
    private final VectorStoreService vectorStoreService;

    /**
     * 分页查询知识文档
     */
    public IPage<AiKnowledge> page(long page, long size, String category, String status) {
        LambdaQueryWrapper<AiKnowledge> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(AiKnowledge::getCategory, category);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(AiKnowledge::getStatus, KnowledgeStatusEnum.valueOf(status));
        } else {
            wrapper.eq(AiKnowledge::getStatus, KnowledgeStatusEnum.ACTIVE);
        }
        wrapper.orderByAsc(AiKnowledge::getCategory)
                .orderByAsc(AiKnowledge::getTitle);
        return knowledgeMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 获取文档详情（含文件内容）
     */
    public Map<String, Object> getDetail(String id) {
        AiKnowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) return null;

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", knowledge.getId());
        result.put("title", knowledge.getTitle());
        result.put("category", knowledge.getCategory());
        result.put("filePath", knowledge.getFilePath());
        result.put("chunkCount", knowledge.getChunkCount());
        result.put("wordCount", knowledge.getWordCount());
        result.put("status", knowledge.getStatus());
        result.put("createTime", knowledge.getCreateTime());

        // 读取文件内容
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get(knowledge.getFilePath());
            if (java.nio.file.Files.exists(filePath)) {
                result.put("content", java.nio.file.Files.readString(filePath));
            }
        } catch (Exception e) {
            log.warn("Failed to read file content for {}: {}", knowledge.getFilePath(), e.getMessage());
        }
        return result;
    }

    /**
     * 从面试目录批量导入文档并向量化（分批处理，防止 OOM）
     *
     * @return 导入统计
     */
    @Transactional(transactionManager = "mysqlTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> importFromDirectory() throws IOException {
        List<Path> mdFiles = knowledgeLoader.scanMarkdownFiles();
        if (mdFiles.isEmpty()) {
            return Map.of("totalFiles", 0, "totalChunks", 0, "message", "No markdown files found");
        }

        // 过滤掉已导入的文件
        List<Path> newFiles = new ArrayList<>();
        for (Path filePath : mdFiles) {
            Long exists = knowledgeMapper.selectCount(
                    new LambdaQueryWrapper<AiKnowledge>()
                            .eq(AiKnowledge::getFilePath, filePath.toString())
                            .eq(AiKnowledge::getStatus, KnowledgeStatusEnum.ACTIVE)
            );
            if (exists > 0) {
                log.info("Skipping already imported: {}", filePath.getFileName());
            } else {
                newFiles.add(filePath);
            }
        }

        if (newFiles.isEmpty()) {
            return Map.of("totalFiles", 0, "totalChunks", 0, "message", "All files already imported");
        }

        // 分批处理：每批 BATCH_SIZE 个文件，处理完立即写 DB 和向量库，释放内存
        final int BATCH_SIZE = 20;
        int totalFiles = 0, totalChunks = 0;

        for (int i = 0; i < newFiles.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, newFiles.size());
            List<Path> batch = newFiles.subList(i, end);

            List<AiKnowledge> batchKnowledge = new ArrayList<>();
            List<Document> batchDocuments = new ArrayList<>();

            for (Path filePath : batch) {
                String category = filePath.getParent().getFileName().toString();
                String fileName = filePath.getFileName().toString();
                try {
                    List<Document> chunks = knowledgeLoader.parseFile(filePath, category);
                    int wordCount = chunks.stream().mapToInt(d -> d.getText().length()).sum();

                    batchKnowledge.add(AiKnowledge.builder()
                            .title(fileName.replace(".md", ""))
                            .category(category)
                            .filePath(filePath.toString())
                            .status(KnowledgeStatusEnum.ACTIVE)
                            .chunkCount(chunks.size())
                            .wordCount(wordCount)
                            .createTime(LocalDateTime.now())
                            .updateTime(LocalDateTime.now())
                            .build());

                    batchDocuments.addAll(chunks);
                    totalChunks += chunks.size();
                    log.info("Parsed {}: {} chunks, ~{} chars", fileName, chunks.size(), wordCount);
                } catch (Exception e) {
                    log.error("Failed to parse file: {}", fileName, e);
                }
            }

            // 当前批次写入 DB
            if (!batchKnowledge.isEmpty()) {
                for (AiKnowledge k : batchKnowledge) {
                    knowledgeMapper.insert(k);
                }
                totalFiles += batchKnowledge.size();
                log.info("Batch {}/{}: saved {} files to DB", (i / BATCH_SIZE) + 1,
                        (newFiles.size() + BATCH_SIZE - 1) / BATCH_SIZE, batchKnowledge.size());
            }

            // 当前批次写入向量库
            if (!batchDocuments.isEmpty()) {
                vectorStoreService.addDocuments(batchDocuments);
                log.info("Batch {}/{}: wrote {} chunks to vector store", (i / BATCH_SIZE) + 1,
                        (newFiles.size() + BATCH_SIZE - 1) / BATCH_SIZE, batchDocuments.size());
            }

            // 主动释放引用，帮助 GC
            batchKnowledge.clear();
            batchDocuments.clear();
        }

        log.info("Import completed: {} files, {} chunks total", totalFiles, totalChunks);
        return Map.of(
                "totalFiles", totalFiles,
                "totalChunks", totalChunks,
                "message", "Successfully imported " + totalFiles + " files, " + totalChunks + " chunks"
        );
    }

    /**
     * 重新加载：清空并重新导入
     */
    @Transactional(transactionManager = "mysqlTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> reload() throws IOException {
        log.info("Reloading knowledge base...");

        // 标记所有文档为已删除
        AiKnowledge update = new AiKnowledge();
        update.setStatus(KnowledgeStatusEnum.DELETED);
        update.setUpdateTime(LocalDateTime.now());
        knowledgeMapper.update(update, new LambdaQueryWrapper<>());

        // 重新导入
        return importFromDirectory();
    }

    /**
     * 删除文档（软删除）
     */
    public void delete(String id) {
        AiKnowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge != null) {
            knowledge.setStatus(KnowledgeStatusEnum.DELETED);
            knowledge.setUpdateTime(LocalDateTime.now());
            knowledgeMapper.updateById(knowledge);
        }
    }

    /**
     * 语义搜索
     */
    public List<Map<String, Object>> search(String query, int topK, String category) {
        List<Document> results;
        if (StrUtil.isNotBlank(category)) {
            results = vectorStoreService.searchWithFilter(query, topK, Map.of("category", category));
        } else {
            results = vectorStoreService.search(query, topK);
        }

        return results.stream()
                .map(doc -> {
                    var metadata = doc.getMetadata();
                    // PGVector 返回的是余弦距离（0=完全相同, 越大越不相关），转换为百分比相关度
                    double distance = ((Number) metadata.getOrDefault("distance", 1.0)).doubleValue();
                    double score = Math.max(0.0, 1.0 - distance);
                    return (Map<String, Object>) Map.of(
                            "content", doc.getText(),
                            "title", metadata.getOrDefault("title", ""),
                            "category", metadata.getOrDefault("category", ""),
                            "filePath", metadata.getOrDefault("filePath", ""),
                            "score", score
                    );
                })
                .toList();
    }
}
