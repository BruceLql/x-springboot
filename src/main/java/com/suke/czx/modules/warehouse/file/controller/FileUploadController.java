package com.suke.czx.modules.warehouse.file.controller;

import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.oss.cloud.ICloudStorage;
import com.suke.czx.modules.oss.cloud.OSSFactory;
import com.suke.czx.modules.oss.entity.SysOss;
import com.suke.czx.modules.oss.service.SysOssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
 * 仓储文件上传Controller
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouse/file")
@Tag(name = "FileUploadController", description = "仓储文件上传")
public class FileUploadController extends AbstractController {

    private final OSSFactory ossFactory;
    private final SysOssService sysOssService;

    /**
     * 上传文件
     */
    @PostMapping(value = "/upload")
    @Operation(summary = "文件上传", description = "上传文件并返回可访问URL")
    public R upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new RRException("上传文件不能为空");
        }

        try (ICloudStorage cloudStorage = ossFactory.build("aliyun")) {
            if (cloudStorage == null) {
                throw new RRException("文件存储未配置，请先配置OSS");
            }

            // 上传文件
            String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String url = cloudStorage.upload(file, suffix);

            // 保存文件信息
            SysOss ossEntity = new SysOss();
            ossEntity.setUrl(url);
            ossEntity.setCreateDate(new Date());
            sysOssService.save(ossEntity);

            log.info("文件上传成功: url={}", url);
            return R.ok().setData(url);
        }
    }

}
