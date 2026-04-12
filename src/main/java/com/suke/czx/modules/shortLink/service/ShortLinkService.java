package com.suke.czx.modules.shortLink.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.suke.czx.modules.shortLink.entity.ShortLink;
import com.baomidou.mybatisplus.extension.service.IService;
import com.suke.czx.modules.shortLink.vo.ShortLinkRequest;
import com.suke.czx.modules.shortLink.vo.ShortLinkResponse;

import java.util.Map;

/**
 * 短链管理服务接口
 *
 * @author 李启岚
 * @Date 2026/4/11
 */
public interface ShortLinkService extends IService<ShortLink> {

    /**
     * 生成短链
     *
     * @param request 短链生成请求
     * @return 短链响应信息
     */
    ShortLinkResponse generateShortLink(ShortLinkRequest request);

    /**
     * 根据短链码获取原始URL
     *
     * @param shortCode 短链码
     * @return 原始URL
     */
    String getOriginalUrl(String shortCode);

    /**
     * 增加访问次数
     *
     * @param shortCode 短链码
     */
    void incrementVisitCount(String shortCode);

    /**
     * 分页查询短链列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    IPage<ShortLinkResponse> pageList(Map<String, Object> params);

    /**
     * 获取短链详情
     *
     * @param linkId 短链ID
     * @return 短链详情
     */
    ShortLinkResponse getDetail(String linkId);

    /**
     * 更新短链信息
     *
     * @param request 短链更新请求
     */
    void updateShortLink(ShortLinkRequest request);

    /**
     * 删除短链
     *
     * @param linkId 短链ID
     */
    void deleteShortLink(String linkId);

    /**
     * 禁用/启用短链
     *
     * @param linkId 短链ID
     * @param status 状态 0-禁用 1-启用
     */
    void updateStatus(String linkId, Integer status);

    /**
     * 根据短链码获取短链信息
     *
     * @param shortCode 短链码
     * @return 短链信息
     */
    ShortLink getByShortCode(String shortCode);
}
