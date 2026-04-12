package com.suke.czx.modules.shortLink.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.suke.czx.common.utils.MPPageConvert;
import com.suke.czx.modules.shortLink.entity.ShortLink;
import com.suke.czx.modules.shortLink.mapper.ShortLinkMapper;
import com.suke.czx.modules.shortLink.service.ShortLinkService;
import com.suke.czx.modules.shortLink.vo.ShortLinkRequest;
import com.suke.czx.modules.shortLink.vo.ShortLinkResponse;
import com.suke.czx.modules.tenancy.entity.TbPlatformTenancy;
import com.suke.czx.modules.tenancy.service.TbPlatformTenancyService;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 短链管理服务实现
 *
 * @author 李启岚
 * @Date 2026/4/11
 */
@Service
@AllArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLink> implements ShortLinkService {

    private final MPPageConvert mpPageConvert;
    private final TbPlatformTenancyService tbPlatformTenancyService;

    // 短链字符集（62个字符：大小写字母+数字）
    private static final String BASE62_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = BASE62_CHARS.length();
    private static final int SHORT_CODE_LENGTH = 6;

    // 安全随机数生成器
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // 原子计数器 - 用于混合生成
    private final AtomicLong counter = new AtomicLong(0);

    // 本地缓存：短链码 -> 原始URL
    private final ConcurrentHashMap<String, String> cacheMap = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShortLinkResponse generateShortLink(ShortLinkRequest request) {
        if (StrUtil.isEmpty(request.getOriginalUrl())) {
            throw new IllegalArgumentException("原始URL不能为空");
        }

        ShortLink shortLink = new ShortLink();
        
        // 生成或使用自定义短链码
        String shortCode;
        if (Boolean.TRUE.equals(request.getCustomCode()) && StrUtil.isNotEmpty(request.getShortCode())) {
            shortCode = request.getShortCode();
            // 检查短链码是否已存在
            QueryWrapper<ShortLink> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(ShortLink::getShortCode, shortCode);
            if (this.count(queryWrapper) > 0) {
                throw new IllegalArgumentException("短链码已存在，请使用其他短链码");
            }
        } else {
            shortCode = this.generateRandomShortCode();
        }

        shortLink.setOriginalUrl(request.getOriginalUrl());
        shortLink.setShortCode(shortCode);
        shortLink.setShortUrl(buildShortUrl(shortCode));
        shortLink.setTenancyId(request.getTenancyId());
        shortLink.setVisitCount(0);
        shortLink.setStatus(1);
        shortLink.setExpireTime(request.getExpireTime());
        shortLink.setUserId(getCurrentUserId());
        shortLink.setCreateTime(new Date());
        shortLink.setRemark(request.getRemark());

        this.save(shortLink);

        // 添加到缓存
        cacheMap.put(shortCode, request.getOriginalUrl());

        return convertToResponse(shortLink);
    }

    @Override
    public String getOriginalUrl(String shortCode) {
        if (StrUtil.isEmpty(shortCode)) {
            return null;
        }

        // 先从缓存获取
        String originalUrl = cacheMap.get(shortCode);
        if (originalUrl != null) {
            return originalUrl;
        }

        // 从数据库获取
        ShortLink shortLink = this.getByShortCode(shortCode);
        if (shortLink != null) {
            // 检查状态
            if (shortLink.getStatus() == 0) {
                throw new RuntimeException("短链已被禁用");
            }
            
            // 检查过期时间
            if (shortLink.getExpireTime() != null && shortLink.getExpireTime().before(new Date())) {
                throw new RuntimeException("短链已过期");
            }

            originalUrl = shortLink.getOriginalUrl();
            // 加入缓存
            cacheMap.put(shortCode, originalUrl);
            return originalUrl;
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementVisitCount(String shortCode) {
        QueryWrapper<ShortLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ShortLink::getShortCode, shortCode);
        ShortLink shortLink = this.getOne(queryWrapper);
        
        if (shortLink != null) {
            shortLink.setVisitCount(shortLink.getVisitCount() + 1);
            shortLink.setUpdateTime(new Date());
            this.updateById(shortLink);
        }
    }

    @Override
    public IPage<ShortLinkResponse> pageList(Map<String, Object> params) {
        QueryWrapper<ShortLink> queryWrapper = new QueryWrapper<>();
        
        // 关键词搜索
        String keyword = mpPageConvert.getKeyword(params);
        if (StrUtil.isNotEmpty(keyword)) {
            queryWrapper.lambda().and(func -> 
                func.like(ShortLink::getShortCode, keyword)
                    .or()
                    .like(ShortLink::getOriginalUrl, keyword)
                    .or()
                    .like(ShortLink::getRemark, keyword)
            );
        }

        // 租户ID过滤
        String tenancyId = (String) params.get("tenancyId");
        if (StrUtil.isNotEmpty(tenancyId)) {
            queryWrapper.lambda().eq(ShortLink::getTenancyId, tenancyId);
        }

        // 状态过滤
        String status = (String) params.get("status");
        if (StrUtil.isNotEmpty(status)) {
            queryWrapper.lambda().eq(ShortLink::getStatus, status);
        }

        // 按创建时间降序
        queryWrapper.lambda().orderByDesc(ShortLink::getCreateTime);

        IPage<ShortLink> page = this.page(mpPageConvert.<ShortLink>pageParamConvert(params), queryWrapper);
        
        // 转换为响应对象
        IPage<ShortLinkResponse> responsePage = page.convert(this::convertToResponse);
        
        // 填充租户信息
        fillTenancyInfo(responsePage);
        
        return responsePage;
    }

    @Override
    public ShortLinkResponse getDetail(String linkId) {
        ShortLink shortLink = this.getById(linkId);
        if (shortLink == null) {
            throw new RuntimeException("短链不存在");
        }
        
        ShortLinkResponse response = convertToResponse(shortLink);
        fillTenancyInfo(response);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShortLink(ShortLinkRequest request) {
        QueryWrapper<ShortLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ShortLink::getShortCode, request.getShortCode());
        ShortLink shortLink = this.getOne(queryWrapper);
        
        if (shortLink == null) {
            throw new RuntimeException("短链不存在");
        }

        if (StrUtil.isNotEmpty(request.getOriginalUrl())) {
            shortLink.setOriginalUrl(request.getOriginalUrl());
            // 更新缓存
            cacheMap.put(shortLink.getShortCode(), request.getOriginalUrl());
        }
        
        if (request.getExpireTime() != null) {
            shortLink.setExpireTime(request.getExpireTime());
        }
        
        if (StrUtil.isNotEmpty(request.getRemark())) {
            shortLink.setRemark(request.getRemark());
        }
        
        shortLink.setUpdateTime(new Date());
        this.updateById(shortLink);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShortLink(String linkId) {
        ShortLink shortLink = this.getById(linkId);
        if (shortLink != null) {
            // 从缓存中移除
            cacheMap.remove(shortLink.getShortCode());
            // 从数据库删除
            this.removeById(linkId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String linkId, Integer status) {
        ShortLink shortLink = this.getById(linkId);
        if (shortLink == null) {
            throw new RuntimeException("短链不存在");
        }
        
        shortLink.setStatus(status);
        shortLink.setUpdateTime(new Date());
        this.updateById(shortLink);
        
        // 如果禁用，从缓存中移除
        if (status == 0) {
            cacheMap.remove(shortLink.getShortCode());
        }
    }

    @Override
    public ShortLink getByShortCode(String shortCode) {
        if (StrUtil.isEmpty(shortCode)) {
            return null;
        }
        
        QueryWrapper<ShortLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ShortLink::getShortCode, shortCode);
        return this.getOne(queryWrapper);
    }

    /**
     * 生成随机短链码 - 多重随机策略，避免可预测性
     * 策略：时间戳 + 随机数 + 计数器混合，然后打乱顺序
     */
    private String generateRandomShortCode() {
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            String shortCode = doGenerateRandomShortCode();
            
            // 检查唯一性
            QueryWrapper<ShortLink> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(ShortLink::getShortCode, shortCode);
            if (this.count(queryWrapper) == 0) {
                return shortCode;
            }
        }
        
        // 极端情况下，使用UUID确保唯一性
        return generateUniqueShortCodeFromUUID();
    }

    /**
     * 执行随机短链码生成
     */
    private String doGenerateRandomShortCode() {
        // 策略1: 基于时间戳和随机数的混合
        long timestamp = System.currentTimeMillis();
        long randomValue = SECURE_RANDOM.nextLong() & Long.MAX_VALUE;
        long counterValue = counter.incrementAndGet();
        
        // 混合多个熵源
        long mixed = timestamp ^ randomValue ^ (counterValue * 2654435761L);
        
        // 转换为62进制
        String code = encodeToBase62(Math.abs(mixed));
        
        // 如果长度不足，补充随机字符
        if (code.length() < SHORT_CODE_LENGTH) {
            code = padLeftWithRandom(code, SHORT_CODE_LENGTH);
        } else if (code.length() > SHORT_CODE_LENGTH) {
            // 如果过长，截取并重新混合
            code = code.substring(0, SHORT_CODE_LENGTH);
        }
        
        // 打乱字符顺序，增加随机性
        code = shuffleString(code);
        
        return code;
    }

    /**
     * 使用UUID生成唯一短链码（备用方案）
     */
    private String generateUniqueShortCodeFromUUID() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // 取UUID的一部分并转换为62进制
        long hash = uuid.hashCode() & Long.MAX_VALUE;
        String code = encodeToBase62(hash);
        
        if (code.length() < SHORT_CODE_LENGTH) {
            code = padLeftWithRandom(code, SHORT_CODE_LENGTH);
        } else if (code.length() > SHORT_CODE_LENGTH) {
            code = code.substring(0, SHORT_CODE_LENGTH);
        }
        
        return shuffleString(code);
    }

    /**
     * 将长整型编码为62进制字符串
     */
    private String encodeToBase62(long number) {
        if (number == 0) {
            return String.valueOf(BASE62_CHARS.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        while (number > 0) {
            int remainder = (int) (number % BASE);
            sb.insert(0, BASE62_CHARS.charAt(remainder));
            number /= BASE;
        }
        return sb.toString();
    }

    /**
     * 左填充随机字符到指定长度
     */
    private String padLeftWithRandom(String str, int length) {
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = str.length(); i < length; i++) {
            sb.append(BASE62_CHARS.charAt(SECURE_RANDOM.nextInt(BASE)));
        }
        sb.append(str);
        return sb.toString();
    }

    /**
     * 打乱字符串字符顺序（Fisher-Yates洗牌算法）
     */
    private String shuffleString(String input) {
        char[] characters = input.toCharArray();
        for (int i = characters.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            // 交换字符
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }
        return new String(characters);
    }

    /**
     * 构建完整短链接
     */
    private String buildShortUrl(String shortCode) {
        // 这里可以根据实际域名配置
        return "http://s.example.com/" + shortCode;
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            return com.suke.czx.common.utils.UserUtil.getUserId();
        } catch (Exception e) {
            return "system";
        }
    }

    /**
     * 转换为响应对象
     */
    private ShortLinkResponse convertToResponse(ShortLink shortLink) {
        ShortLinkResponse response = new ShortLinkResponse();
        BeanUtils.copyProperties(shortLink, response);
        return response;
    }

    /**
     * 填充租户信息
     */
    private void fillTenancyInfo(IPage<ShortLinkResponse> page) {
        if (page != null && page.getRecords() != null) {
            page.getRecords().forEach(this::fillTenancyInfo);
        }
    }

    /**
     * 填充租户信息
     */
    private void fillTenancyInfo(ShortLinkResponse response) {
        if (response != null && response.getTenancyId() != null) {
            TbPlatformTenancy tenancy = tbPlatformTenancyService.getById(response.getTenancyId());
            if (tenancy != null) {
                response.setTenancyName(tenancy.getTenancyName());
            }
        }
    }
}
