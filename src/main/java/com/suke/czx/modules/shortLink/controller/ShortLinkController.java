package com.suke.czx.modules.shortLink.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.suke.czx.common.annotation.AuthIgnore;
import com.suke.czx.common.annotation.ResourceAuth;
import com.suke.czx.common.annotation.SysLog;
import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.shortLink.service.ShortLinkService;
import com.suke.czx.modules.shortLink.vo.ShortLinkQueryByIdRequest;
import com.suke.czx.modules.shortLink.vo.ShortLinkRequest;
import com.suke.czx.modules.shortLink.vo.ShortLinkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * 短链管理
 *
 * @author 李启岚
 * @Date 2026/4/11
 */
@RestController
@AllArgsConstructor
@RequestMapping("/shortlink")
@Tag(name = "ShortLinkController", description = "短链管理")
public class ShortLinkController extends AbstractController {

    private final ShortLinkService shortLinkService;

    /**
     * 生成短链
     */
    @SysLog("生成短链")
    @PostMapping("/generate")
    @ResourceAuth(value = "生成短链", module = "短链管理")
    @Operation(summary = "生成短链", description = "根据原始URL生成短链")
    public R generate(@RequestBody ShortLinkRequest request) {
        ShortLinkResponse response = shortLinkService.generateShortLink(request);
        return R.ok("生成成功").setData(response);
    }

    /**
     * 短链列表
     */
    @GetMapping("/list")
    @ResourceAuth(value = "短链列表", module = "短链管理")
    @Operation(summary = "短链列表", description = "分页查询短链列表")
    public R list(@RequestParam Map<String, Object> params) {
        IPage<ShortLinkResponse> page = shortLinkService.pageList(params);
        return R.ok().setData(page);
    }

    /**
     * 短链详情
     */
    @GetMapping("/detail/{linkId}")
    @ResourceAuth(value = "短链详情", module = "短链管理")
    @Operation(summary = "短链详情", description = "获取短链详细信息")
    public R detail(@PathVariable String linkId) {
        ShortLinkResponse response = shortLinkService.getDetail(linkId);
        return R.ok().setData(response);
    }

    /**
     * 更新短链
     */
    @SysLog("更新短链")
    @PostMapping("/update")
    @ResourceAuth(value = "更新短链", module = "短链管理")
    @Operation(summary = "更新短链", description = "更新短链信息")
    public R update(@RequestBody ShortLinkRequest request) {
        shortLinkService.updateShortLink(request);
        return R.ok("更新成功");
    }

    /**
     * 删除短链
     */
    @SysLog("删除短链")
    @PostMapping("/delete")
    @ResourceAuth(value = "删除短链", module = "短链管理")
    @Operation(summary = "删除短链", description = "删除指定短链")
    public R delete(@RequestBody ShortLinkQueryByIdRequest request) {
        shortLinkService.deleteShortLink(request.getLinkId());
        return R.ok("删除成功");
    }

    /**
     * 禁用/启用短链
     */
    @SysLog("更新短链状态")
    @PostMapping("/updateStatus")
    @ResourceAuth(value = "更新短链状态", module = "短链管理")
    @Operation(summary = "更新短链状态", description = "禁用或启用短链")
    public R updateStatus(@RequestBody ShortLinkQueryByIdRequest request) {
        shortLinkService.updateStatus(request.getLinkId(), request.getStatus());
        return R.ok("操作成功");
    }

    /**
     * 短链跳转（无需认证）
     */
    @AuthIgnore
    @GetMapping("/{shortCode}")
    @Operation(summary = "短链跳转", description = "根据短链码跳转到原始URL")
    public void redirect(@PathVariable String shortCode, HttpServletResponse response) throws IOException {
        try {
            // 获取原始URL
            String originalUrl = shortLinkService.getOriginalUrl(shortCode);

            if (StrUtil.isEmpty(originalUrl)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("短链不存在或已失效");
                return;
            }

            // 增加访问次数
            shortLinkService.incrementVisitCount(shortCode);

            // 重定向到原始URL
            response.sendRedirect(originalUrl);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("跳转失败: " + e.getMessage());
        }
    }

    /**
     * 获取短链统计信息
     */
    @GetMapping("/stats/{linkId}")
    @ResourceAuth(value = "短链统计", module = "短链管理")
    @Operation(summary = "短链统计", description = "获取短链访问统计信息")
    public R stats(@PathVariable String linkId) {
        ShortLinkResponse response = shortLinkService.getDetail(linkId);
        Map<String, Object> stats = MapUtil.newHashMap();
        stats.put("visitCount", response.getVisitCount());
        stats.put("createTime", response.getCreateTime());
        stats.put("status", response.getStatus());
        return R.ok().setData(stats);
    }
}
