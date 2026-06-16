package com.suke.czx.modules.ai.interfaces.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.suke.czx.common.annotation.AuthIgnore;
import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.ai.application.service.KnowledgeAppService;
import com.suke.czx.modules.ai.domain.entity.AiKnowledge;
import com.suke.czx.modules.ai.infrastructure.convert.AiConvert;
import com.suke.czx.modules.ai.interfaces.dto.query.KnowledgePageQuery;
import com.suke.czx.modules.ai.interfaces.dto.query.KnowledgeSearchQuery;
import com.suke.czx.modules.ai.interfaces.vo.KnowledgeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库管理 Controller
 */
@RestController
@RequestMapping("/ai/knowledge")
@RequiredArgsConstructor
@Tag(name = "AI知识库", description = "知识文档导入、管理、语义搜索")
public class KnowledgeController extends AbstractController {

    private final KnowledgeAppService knowledgeAppService;
    private final AiConvert aiConvert;

    @GetMapping("/list")
    @Operation(summary = "分页查询知识文档")
    public R list(KnowledgePageQuery query) {
        IPage<AiKnowledge> page = knowledgeAppService.page(
                query.getPage(), query.getSize(),
                query.getCategory(), query.getStatus()
        );
        List<KnowledgeVO> vos = aiConvert.toKnowledgeVOList(page.getRecords());
        return R.ok().setData(vos).put("page", page);
    }

    @GetMapping("/info/{id}")
    @Operation(summary = "获取文档详情（含完整内容）")
    public R info(@Parameter(description = "文档ID") @PathVariable String id) {
        Map<String, Object> detail = knowledgeAppService.getDetail(id);
        if (detail == null) {
            return R.error("文档不存在");
        }
        return R.ok().setData(detail);
    }

    @PostMapping("/import")
    @Operation(summary = "从面试目录批量导入文档并向量化")
    @AuthIgnore
    public R importKnowledge() {
        try {
            Map<String, Object> result = knowledgeAppService.importFromDirectory();
            return R.ok().setData(result);
        } catch (Exception e) {
            return R.error("导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/reload")
    @Operation(summary = "清空并重新导入")
    @AuthIgnore
    public R reload() {
        try {
            Map<String, Object> result = knowledgeAppService.reload();
            return R.ok().setData(result);
        } catch (Exception e) {
            return R.error("重载失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档")
    public R delete(@Parameter(description = "文档ID") @PathVariable String id) {
        knowledgeAppService.delete(id);
        return R.ok("删除成功");
    }

    @PostMapping("/search")
    @Operation(summary = "语义搜索")
    @AuthIgnore
    public R search(@RequestBody @Valid KnowledgeSearchQuery query) {
        List<Map<String, Object>> results = knowledgeAppService.search(
                query.getQuery(), query.getTopK(), query.getCategory()
        );
        return R.ok().setData(results);
    }
}
