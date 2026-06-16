package com.suke.czx.modules.live.application.service;

import com.suke.czx.common.lock.RedissonLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 直播任务定时任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveTaskScheduler {

    private final LiveTaskApplicationService liveTaskApplicationService;
    private final RedissonLock redissonLock;

    private static final String EXPIRE_CHECK_LOCK_KEY = "live:scheduler:expireCheck";
    private static final String PENDING_CHECK_LOCK_KEY = "live:scheduler:pendingCheck";

    /**
     * 每分钟检查一次过期任务（分布式锁保证单实例执行）
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkExpiredTasks() {
        if (!redissonLock.lock(EXPIRE_CHECK_LOCK_KEY, 5, 30)) {
            return;
        }
        try {
            liveTaskApplicationService.autoStopExpiredTasks();
        } catch (Exception e) {
            log.error("定时检查过期任务异常", e);
        } finally {
            redissonLock.unlock(EXPIRE_CHECK_LOCK_KEY);
        }
    }

    /**
     * 每分钟检查 PENDING 任务，检测到开播自动启动监控
     */
    @Scheduled(cron = "15 * * * * ?")
    public void checkPendingTasks() {
        if (!redissonLock.lock(PENDING_CHECK_LOCK_KEY, 5, 30)) {
            return;
        }
        try {
            liveTaskApplicationService.autoStartPendingTasks();
        } catch (Exception e) {
            log.error("定时检查PENDING任务异常", e);
        } finally {
            redissonLock.unlock(PENDING_CHECK_LOCK_KEY);
        }
    }
}
