package com.suke.czx.modules.ai.infrastructure.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Markdown 知识库加载器
 * <p>
 * 从面试文档目录递归扫描 .md 文件，按 ## 标题分块，
 * 每块生成一个 Spring AI Document（含元数据）
 */
@Slf4j
@Component
public class MarkdownKnowledgeLoader {

    @Value("${ai.knowledge.base-path}")
    private String basePath;

    @PostConstruct
    public void init() {
        Path root = Paths.get(basePath);
        log.info("Knowledge base path configured as: '{}'", basePath);
        log.info("Knowledge base absolute path: {}", root.toAbsolutePath());
        if (!Files.exists(root)) {
            log.warn("Knowledge base directory does NOT exist: {}", root.toAbsolutePath());
        } else {
            try (Stream<Path> files = Files.list(root)) {
                long count = files.count();
                log.info("Knowledge base directory exists with {} top-level entries", count);
            } catch (IOException e) {
                log.warn("Cannot list knowledge base directory: {}", e.getMessage());
            }
        }
    }

    @Value("${ai.knowledge.chunk-size:1000}")
    private int chunkSize;

    @Value("${ai.knowledge.chunk-overlap:100}")
    private int chunkOverlap;

    /**
     * 扫描并加载所有文档文件
     *
     * @return 文件路径列表（相对于 basePath）
     */
    public List<Path> scanMarkdownFiles() throws IOException {
        Path root = Paths.get(basePath);
        if (!Files.exists(root)) {
            log.warn("Knowledge base path does not exist: {}", basePath);
            return List.of();
        }

        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .toList();
        }
    }

    /**
     * 读取单个 markdown 文件的内容
     */
    public String readFile(Path filePath) throws IOException {
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    /**
     * 将 markdown 内容按 ## 标题分块
     */
    public List<String> splitBySections(String content) {
        List<String> sections = new ArrayList<>();
        StringBuilder currentSection = new StringBuilder();

        for (String line : content.split("\n")) {
            if (line.trim().startsWith("## ") && !currentSection.isEmpty()) {
                sections.add(currentSection.toString().trim());
                currentSection = new StringBuilder();
            }
            currentSection.append(line).append("\n");
        }
        if (!currentSection.isEmpty()) {
            sections.add(currentSection.toString().trim());
        }

        return sections;
    }

    /**
     * 将单个文件解析为 Document 列表（含分块）
     *
     * @param filePath  文件路径（相对于 basePath）
     * @param category  分类（父目录名）
     * @return Document 列表
     */
    public List<Document> parseFile(Path filePath, String category) throws IOException {
        String content = readFile(filePath);
        List<String> sections = splitBySections(content);

        List<Document> documents = new ArrayList<>();
        String fileName = filePath.getFileName().toString();
        String relativePath = basePath != null ?
                Paths.get(basePath).relativize(filePath).toString() : fileName;

        for (int i = 0; i < sections.size(); i++) {
            String section = sections.get(i);
            if (section.length() < 50) continue; // 跳过过短的节

            // 提取标题（第一个 ## 行）
            String title = extractTitle(fileName, section, i);

            // 如果 section 过长，进一步切分
            if (section.length() > chunkSize) {
                List<String> subChunks = splitLongSection(section);
                for (int j = 0; j < subChunks.size(); j++) {
                    documents.add(buildDocument(
                            subChunks.get(j),
                            title + " (part" + (j + 1) + ")",
                            category,
                            relativePath,
                            i * 100 + j
                    ));
                }
            } else {
                documents.add(buildDocument(section, title, category, relativePath, i));
            }
        }

        return documents;
    }

    /**
     * 将过长的 section 按固定大小切分
     */
    private List<String> splitLongSection(String content) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int len = content.length();
        while (start < len) {
            int end = Math.min(start + chunkSize, len);
            if (end < len) {
                int lastNewline = content.lastIndexOf('\n', end);
                if (lastNewline > start + chunkSize / 2) {
                    end = lastNewline;
                }
            }
            chunks.add(content.substring(start, end));
            // 计算下一个起点，避免死循环：确保 start 始终前进
            int nextStart = end - chunkOverlap;
            if (nextStart <= start) {
                start = end; // 剩余内容不足一次 overlap，直接跳到 end 避免卡死
            } else {
                start = nextStart;
            }
        }
        return chunks;
    }

    private String extractTitle(String fileName, String section, int index) {
        for (String line : section.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("## ")) {
                return trimmed.substring(3).trim();
            }
        }
        return fileName.replace(".md", "") + "-section" + (index + 1);
    }

    private Document buildDocument(String content, String title, String category,
                                    String filePath, int chunkIndex) {
        return new Document(
                content,
                Map.of(
                        "title", title,
                        "category", category,
                        "filePath", filePath,
                        "chunkIndex", String.valueOf(chunkIndex)
                )
        );
    }
}
