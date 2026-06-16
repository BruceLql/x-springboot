package com.suke.czx.modules.oss.cloud;


import com.suke.czx.common.exception.RRException;
import com.suke.czx.common.utils.LeafSnowflakeGenerator;
import com.suke.czx.common.utils.SpringContextUtils;
import com.suke.czx.modules.oss.entity.SysOssSetting;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author 李启岚
 * @Date 2026/4/25 10:02
 */
@Slf4j
public class AliCloudStorageService implements ICloudStorage {
    private final OSS client;
    private final SysOssSetting config;
    private final LeafSnowflakeGenerator leafSnowflakeGenerator;

    public AliCloudStorageService(SysOssSetting config) {
        this.config = config;
        this.leafSnowflakeGenerator = SpringContextUtils.getBean(LeafSnowflakeGenerator.class);
        try {
//            String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
//            String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
            String accessKeyId = config.accessKey;
            String accessKeySecret = config.secretKey;


            // 设置OSS地域和Endpoint
            String region = config.getRegion();
            String endpoint = config.getUrl();

            // 创建凭证提供者
            DefaultCredentialProvider provider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);

            // 配置客户端参数
            ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
            // 显式声明使用V4签名算法
            clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

            // 初始化OSS客户端
            client = OSSClientBuilder.create()
                    .credentialsProvider(provider)
                    .clientConfiguration(clientBuilderConfiguration)
                    .region(region)
                    .endpoint(endpoint)
                    .build();
        } catch (Exception e) {
            log.error("aliOSS 初始化失败：{}", e.getMessage());
            throw new RRException("aliOSS初始化失败");
        }
    }

    @Override
    public String upload(MultipartFile file, String objectName) {
        String fileName = leafSnowflakeGenerator.nextId() + objectName;
        log.info("ali 文件上传开始：{}", fileName);

        try {
            client.putObject(config.getBucketName(), fileName, new ByteArrayInputStream(file.getBytes()));
            return config.getView() + "/" + fileName;
        } catch (IOException e) {
            log.error("ali 文件上传失败：{}", e.getMessage());
            throw new RRException("上传文件失败", e);
        }
    }

    @Override
    public String upload(byte[] data, String path) {
        return "";
    }

    @Override
    public String uploadSuffix(byte[] data, String suffix) {
        return "";
    }

    @Override
    public String upload(InputStream inputStream, String path) {
        return "";
    }

    @Override
    public String uploadSuffix(InputStream inputStream, String suffix) {
        return "";
    }

    @Override
    public void close() {
        if (client != null) {
            client.shutdown();
            log.info("aliOSS client 已关闭");
        }
    }
}
