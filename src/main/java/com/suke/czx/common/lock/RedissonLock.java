package com.suke.czx.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 使用Redisson加锁（支持看门狗机制）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLock {

    private final RedissonClient redissonClient;

    /**
     * 获取锁（带看门狗机制，默认30秒自动续期）
     *
     * @param lockKey 锁名
     * @return 是否加锁成功
     */
    public boolean lock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            lock.lock(); // 默认看门狗，30秒自动续期
            return true;
        } catch (Exception e) {
            log.error("[RedissonLock][lock]>>>> 加锁异常: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 获取锁（带看门狗机制，指定等待时间和 leaseTime）
     *
     * @param lockKey   锁名
     * @param waitTime  等待时间（秒）
     * @param leaseTime 租约时间（秒），-1表示启用看门狗
     * @return 是否加锁成功
     */
    public boolean lock(String lockKey, long waitTime, long leaseTime) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            boolean success = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (success) {
                log.debug("[RedissonLock][lock]>>>> 加锁成功: {}", lockKey);
            } else {
                log.warn("[RedissonLock][lock]>>>> 加锁失败: {}", lockKey);
            }
            return success;
        } catch (Exception e) {
            log.error("[RedissonLock][lock]>>>> 加锁异常: {}", e.getMessage());
        }
        return false;
    }

    public boolean lock(String lockKey, long waitTime, TimeUnit timeUnit) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            boolean success = lock.tryLock(waitTime, 30, timeUnit);
            if (success) {
                log.debug("[RedissonLock][lock]>>>> 加锁成功: {}", lockKey);
            } else {
                log.warn("[RedissonLock][lock]>>>> 加锁失败: {}", lockKey);
            }
            return success;
        } catch (Exception e) {
            log.error("[RedissonLock][lock]>>>> 加锁异常: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 获取锁（指定过期时间，不启用看门狗）
     *
     * @param lockKey       锁名
     * @param expireSeconds 过期时间（秒）
     * @return 是否加锁成功
     */
    public boolean lockWithExpire(String lockKey, long expireSeconds) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            lock.lock(expireSeconds, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("[RedissonLock][lockWithExpire]>>>> 加锁异常: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁名
     */
    public void unlock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[RedissonLock][unlock]>>>> 释放锁成功: {}", lockKey);
            } else {
                log.warn("[RedissonLock][unlock]>>>> 当前线程不持有锁: {}", lockKey);
            }
        } catch (Exception e) {
            log.error("[RedissonLock][unlock]>>>> 释放锁异常: {}", e.getMessage());
        }
    }

    /**
     * 检查是否持有锁
     *
     * @param lockKey 锁名
     * @return 是否持有
     */
    public boolean isLocked(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            return lock.isLocked();
        } catch (Exception e) {
            log.error("[RedissonLock][isLocked]>>>> 检查锁状态异常: {}", e.getMessage());
        }
        return false;
    }
}