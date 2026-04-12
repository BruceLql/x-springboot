package com.suke.czx.modules.shortLink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.shortLink.entity.ShortLink;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短链管理
 *
 * @author 李启岚
 * @Date 2026/4/11
 */
@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLink> {

}
