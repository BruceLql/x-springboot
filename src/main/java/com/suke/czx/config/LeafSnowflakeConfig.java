package com.suke.czx.config;

import com.suke.czx.common.utils.LeafSnowflakeGenerator;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 美团Leaf-Snowflake配置
 * <p>
 * Worker ID分配策略（参考Leaf对Zookeeper的使用，改用Redis实现）：
 * 1. 启动时通过Redis INCR原子自增分配唯一Worker ID
 * 2. 定期上报当前时间戳到Redis，用于重启时校验时钟回拨
 * 3. 支持手动配置覆盖自动分配
 */
@Slf4j
@Configuration
public class LeafSnowflakeConfig {

    private static final String WORKER_ID_KEY = "x-springboot:leaf:worker-id-counter";
    private static final String TIMESTAMP_KEY_PREFIX = "x-springboot:leaf:timestamp:";
    private static final int MAX_WORKER_ID = 31;

    /** 时间戳上报间隔（秒） */
    private static final int REPORT_INTERVAL_SECONDS = 3;

    @Value("${leaf.snowflake.datacenter-id:0}")
    private long datacenterId;

    @Value("${leaf.snowflake.worker-id:-1}")
    private long configuredWorkerId;

    private ScheduledExecutorService scheduler;
    private LeafSnowflakeGenerator generator;
    private String timestampKey;

    @Bean
    public LeafSnowflakeGenerator leafSnowflakeGenerator(StringRedisTemplate stringRedisTemplate) {
        long workerId = resolveWorkerId(stringRedisTemplate);
        generator = new LeafSnowflakeGenerator(datacenterId, workerId);

        // 启动时间戳校验（Leaf增强特性）
        timestampKey = TIMESTAMP_KEY_PREFIX + datacenterId + ":" + workerId;
        validateStartupTimestamp(stringRedisTemplate);

        // 定时上报时间戳到Redis（3秒一次）
        startTimestampReporter(stringRedisTemplate);

        return generator;
    }

    /**
     * 解析Worker ID：优先使用配置值，否则通过Redis自动分配
     */
    private long resolveWorkerId(StringRedisTemplate stringRedisTemplate) {
        if (configuredWorkerId >= 0) {
            log.info("使用配置的Worker ID: {}", configuredWorkerId);
            return configuredWorkerId;
        }

        try {
            // 通过Redis INCR原子分配Worker ID（取模确保在0-31范围内）
            Long counter = stringRedisTemplate.opsForValue().increment(WORKER_ID_KEY);
            long workerId = (counter != null ? counter : 0) % (MAX_WORKER_ID + 1);
            log.info("通过Redis自动分配Worker ID: {} (counter={})", workerId, counter);
            return workerId;
        } catch (Exception e) {
            // Redis不可用时，使用IP哈希作为fallback
            long fallbackId = getFallbackWorkerId();
            log.warn("Redis分配Worker ID失败，使用IP哈希fallback: {}", fallbackId, e);
            return fallbackId;
        }
    }

    /**
     * 启动时间戳校验：检测是否存在长时间时钟回拨
     */
    private void validateStartupTimestamp(StringRedisTemplate stringRedisTemplate) {
        try {
            String lastTimestampStr = stringRedisTemplate.opsForValue().get(timestampKey);
            if (lastTimestampStr != null) {
                long lastTimestamp = Long.parseLong(lastTimestampStr);
                if (!generator.validateStartupTimestamp(lastTimestamp)) {
                    log.error("【Leaf告警】启动时间戳校验失败，Redis记录={}, 当前时间={}，差值={}ms",
                            lastTimestamp, System.currentTimeMillis(), lastTimestamp - System.currentTimeMillis());
                    // 不阻止启动，但记录告警（生产环境可改为阻止启动）
                }
            }
        } catch (Exception e) {
            log.warn("启动时间戳校验跳过（Redis不可用）", e);
        }
    }

    /**
     * 定期上报时间戳到Redis（Leaf的核心特性之一）
     * 用于下次启动时检测是否存在时钟回拨
     */
    private void startTimestampReporter(StringRedisTemplate stringRedisTemplate) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "leaf-timestamp-reporter");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                long timestamp = generator.getLastTimestamp();
                if (timestamp > 0) {
                    stringRedisTemplate.opsForValue().set(timestampKey, String.valueOf(timestamp));
                } else {
                    // 还未生成过ID，上报当前时间
                    stringRedisTemplate.opsForValue().set(timestampKey, String.valueOf(System.currentTimeMillis()));
                }
            } catch (Exception e) {
                log.debug("时间戳上报失败（不影响ID生成）", e);
            }
        }, REPORT_INTERVAL_SECONDS, REPORT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * IP哈希作为Worker ID的fallback策略
     */
    private long getFallbackWorkerId() {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            int hash = hostAddress.hashCode();
            return Math.abs(hash) % (MAX_WORKER_ID + 1);
        } catch (Exception e) {
            return 0;
        }
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
