package com.suke.czx.modules.ai.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 向量存储服务
 * <p>
 * 封装 PGVector 操作：写入、检索、删除。
 * 当 PostgreSQL 数据源未启用时，所有方法安全降级为空操作。
 */
@Slf4j
@Service
public class VectorStoreService {

    @Autowired(required = false)
    private VectorStore vectorStore;

    private boolean isAvailable() {
        return vectorStore != null;
    }

    /**
     * 批量写入文档向量
     */
    public void addDocuments(List<Document> documents) {
        if (!isAvailable()) {
            log.warn("PGVector is not available, skipping add {} documents", documents.size());
            return;
        }
        log.info("Writing {} documents to PGVector...", documents.size());
        vectorStore.add(documents);
        log.info("Successfully wrote {} documents to PGVector", documents.size());
    }

    /**
     * 语义检索 top-K 相似文档
     */
    public List<Document> search(String query, int topK) {
        if (!isAvailable()) {
            log.debug("PGVector is not available, returning empty search results");
            return Collections.emptyList();
        }
        log.debug("Vector search: query='{}', topK={}", truncate(query, 50), topK);
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.3)
                        .build()
        );
    }

    /**
     * 语义检索（带元数据过滤）
     */
    public List<Document> searchWithFilter(String query, int topK, Map<String, Object> filterMetadata) {
        if (!isAvailable()) {
            log.debug("PGVector is not available, returning empty search results");
            return Collections.emptyList();
        }
        var request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.5);

        if (filterMetadata != null && !filterMetadata.isEmpty()) {
            request.filterExpression(buildFilterExpression(filterMetadata));
        }

        return vectorStore.similaritySearch(request.build());
    }

    /**
     * 根据文档ID删除向量
     */
    public void deleteByDocumentId(String documentId) {
        if (!isAvailable()) {
            log.warn("PGVector is not available, skipping delete for document: {}", documentId);
            return;
        }
        log.info("Deleting vectors for document: {}", documentId);
        vectorStore.delete(List.of(documentId));
    }

    private String buildFilterExpression(Map<String, Object> filters) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (var entry : filters.entrySet()) {
            if (i > 0) sb.append(" AND ");
            sb.append(entry.getKey()).append(" == '").append(entry.getValue()).append("'");
            i++;
        }
        return sb.toString();
    }

    private String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
