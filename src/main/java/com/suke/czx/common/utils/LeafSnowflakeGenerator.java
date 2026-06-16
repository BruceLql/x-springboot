package com.suke.czx.common.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * 美团Leaf-Snowflake增强版分布式ID生成器
 * <p>
 * 对比标准Snowflake的增强点：
 * 1. 时钟回拨保护：回拨 < 5ms时自旋等待，>= 5ms时抛异常
 * 2. Worker ID通过Redis自动分配，支持多实例部署无冲突
 * 3. 启动时校验上次时间戳，防止长时间时钟回拨导致ID重复
 * 4. 集中管理为Spring Bean，全局唯一实例
 * <p>
 * ID结构（64位）：
 * - 1 bit:  符号位（始终为0）
 * - 41 bits: 时间戳（毫秒级，相对于自定义纪元，可用约69年）
 * - 5 bits:  数据中心ID（0-31）
 * - 5 bits:  工作节点ID（0-31）
 * - 12 bits: 序列号（同一毫秒内递增，0-4095）
 */
@Slf4j
public class LeafSnowflakeGenerator {

    // ==================== 位分配常量 ====================
    /** 时间戳占用位数 */
    private static final long TIMESTAMP_BITS = 41L;
    /** 数据中心ID占用位数 */
    private static final long DATACENTER_ID_BITS = 5L;
    /** 工作节点ID占用位数 */
    private static final long WORKER_ID_BITS = 5L;
    /** 序列号占用位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 最大数据中心ID */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS); // 31
    /** 最大工作节点ID */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS); // 31
    /** 序列号掩码 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS); // 4095

    // ==================== 位移常量 ====================
    /** 工作节点ID左移位数 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    /** 数据中心ID左移位数 */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    /** 时间戳左移位数 */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /**
     * 自定义纪元：2024-01-01 00:00:00 UTC
     * 从这个时间开始计算时间戳，可使用约69年（到2093年）
     */
    private static final long EPOCH = 1704067200000L;

    /** 时钟回拨容忍阈值（毫秒），小于此值时自旋等待 */
    private static final long MAX_BACKWARD_MS = 5L;

    // ==================== 实例变量 ====================
    private final long datacenterId;
    private final long workerId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 构造器
     *
     * @param datacenterId 数据中心ID (0-31)
     * @param workerId     工作节点ID (0-31)
     */
    public LeafSnowflakeGenerator(long datacenterId, long workerId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("datacenterId不能大于%d或小于0, 当前值: %d", MAX_DATACENTER_ID, datacenterId));
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("workerId不能大于%d或小于0, 当前值: %d", MAX_WORKER_ID, workerId));
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
        log.info("LeafSnowflakeGenerator初始化成功: datacenterId={}, workerId={}", datacenterId, workerId);
    }

    /**
     * 生成下一个分布式ID（线程安全）
     *
     * @return 64位Long型ID
     */
    public synchronized long nextId() {
        long currentTimestamp = currentTimeMillis();

        // 【Leaf增强】时钟回拨检测与处理
        if (currentTimestamp < lastTimestamp) {
            long offset = lastTimestamp - currentTimestamp;
            if (offset <= MAX_BACKWARD_MS) {
                // 回拨在容忍范围内，自旋等待
                try {
                    wait(offset << 1);
                    currentTimestamp = currentTimeMillis();
                    if (currentTimestamp < lastTimestamp) {
                        throw new RuntimeException(
                                String.format("时钟回拨，自旋等待后仍未恢复。回拨时间: %dms", lastTimestamp - currentTimestamp));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("时钟回拨等待被中断", e);
                }
            } else {
                // 回拨超过阈值，直接拒绝
                throw new RuntimeException(
                        String.format("时钟回拨超过容忍阈值(%dms)，拒绝生成ID。回拨时间: %dms", MAX_BACKWARD_MS, offset));
            }
        }

        if (currentTimestamp == lastTimestamp) {
            // 同一毫秒内，序列号递增
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 序列号溢出，等待下一毫秒
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 新的毫秒，序列号归零
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        // 组装ID
        return ((currentTimestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成下一个ID的字符串形式
     *
     * @return ID字符串
     */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    /**
     * 等待直到下一毫秒
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 【Leaf增强】启动时间戳校验
     * 对比Redis中记录的上次时间戳，检测是否存在异常时钟回拨
     *
     * @param lastRecordedTimestamp Redis中记录的上次时间戳
     * @return 校验是否通过
     */
    public boolean validateStartupTimestamp(long lastRecordedTimestamp) {
        long currentTimestamp = currentTimeMillis();
        if (lastRecordedTimestamp > 0 && currentTimestamp < lastRecordedTimestamp) {
            long diff = lastRecordedTimestamp - currentTimestamp;
            log.error("启动时间戳校验失败！当前时间落后于记录时间{}ms，可能存在时钟回拨", diff);
            return false;
        }
        return true;
    }

    /**
     * 获取上次生成ID的时间戳（用于定期持久化到Redis）
     */
    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    public long getWorkerId() {
        return workerId;
    }
}
