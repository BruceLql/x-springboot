package com.suke.czx.modules.live.application.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suke.czx.common.exception.RRException;
import com.suke.czx.common.lock.RedissonLock;
import com.suke.czx.common.utils.Constant;
import com.suke.czx.common.utils.LeafSnowflakeGenerator;
import com.suke.czx.common.utils.UserUtil;
import com.suke.czx.modules.live.domain.command.CreateLiveTaskCommand;
import com.suke.czx.modules.live.domain.command.UpdateLiveTaskCommand;
import com.suke.czx.modules.live.domain.entity.*;
import com.suke.czx.modules.live.domain.enums.TaskStatusEnum;
import com.suke.czx.modules.live.infrastructure.batch.LiveEventBatchProcessor;
import com.suke.czx.modules.live.infrastructure.buffer.LiveEventBuffer;
import com.suke.czx.modules.live.infrastructure.cache.LiveRedisCacheService;
import com.suke.czx.modules.live.infrastructure.websocket.LiveWebSocketPublisher;
import com.suke.czx.modules.live.infrastructure.convert.LiveConvert;
import com.suke.czx.modules.live.infrastructure.repository.*;
import com.suke.czx.modules.live.infrastructure.watcher.DouYinHackLiveRoomWatcherWithStats;
import com.suke.czx.modules.live.interfaces.dto.query.LiveDataPageQuery;
import com.suke.czx.modules.live.interfaces.dto.query.LiveTaskPageQuery;
import com.suke.czx.modules.live.interfaces.vo.*;
import cool.scx.live_room_watcher.impl.douyin_hack.DouYinHackLiveRoomWatcher;
import cool.scx.live_room_watcher.impl.tiktok_hack.TikTokHackLiveRoomWatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 直播任务应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveTaskApplicationService {

    private static final String TASK_NOT_FOUND = "任务不存在";
    private static final String TASK_LOCK_PREFIX = Constant.SYSTEM_NAME + "live:task:lock:";

    private final LiveTaskMapper liveTaskMapper;
    private final LiveChatMapper liveChatMapper;
    private final LiveGiftMapper liveGiftMapper;
    private final LiveLikeMapper liveLikeMapper;
    private final LiveFollowMapper liveFollowMapper;
    private final LiveUserEnterMapper liveUserEnterMapper;
    private final LiveEcomMapper liveEcomMapper;
    private final LiveConvert liveConvert;
    private final LeafSnowflakeGenerator leafSnowflakeGenerator;
    private final LiveRedisCacheService liveRedisCacheService;
    private final LiveEventBuffer eventBuffer;
    private final LiveEventBatchProcessor batchProcessor;
    private final LiveWebSocketPublisher liveWebSocketPublisher;
    private final RedissonLock redissonLock;

    // 存储正在运行的监控任务 <taskId, Watcher>
    private final Map<String, Object> watcherMap = new ConcurrentHashMap<>();
    // 存储监控线程 <taskId, Thread>
    private final Map<String, Thread> threadMap = new ConcurrentHashMap<>();
    // 监控任务线程池（有界，防止OOM）
    private final ExecutorService executorService = new ThreadPoolExecutor(
            2, 10, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            r -> {
                Thread t = new Thread(r, "live-watcher-" + WATCHER_THREAD_COUNTER.incrementAndGet());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy());
    private static final AtomicInteger WATCHER_THREAD_COUNTER = new AtomicInteger(0);

    /**
     * 分页查询任务列表
     */
    public IPage<LiveTaskVO> queryPage(LiveTaskPageQuery query) {
        Page<LiveTask> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<LiveTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getPlatform() != null, LiveTask::getPlatform, query.getPlatform())
                .eq(query.getTaskStatus() != null, LiveTask::getTaskStatus, query.getTaskStatus())
                .like(CharSequenceUtil.isNotBlank(query.getRoomUrl()), LiveTask::getRoomUrl, query.getRoomUrl())
                .like(CharSequenceUtil.isNotBlank(query.getAnchorName()), LiveTask::getAnchorName, query.getAnchorName())
                .orderByDesc(LiveTask::getCreateTime);

        IPage<LiveTask> taskPage = liveTaskMapper.selectPage(page, wrapper);

        // 转换为 VO 并填充统计数据
        return taskPage.convert(task -> {
            LiveTaskVO vo = liveConvert.toVO(task);
            // 如果任务有监听数据，填充统计汇总
            if (task.getTaskStatus() == TaskStatusEnum.MONITORING || task.getTaskStatus() == TaskStatusEnum.STOPPED || task.getTaskStatus() == TaskStatusEnum.INTERRUPTED) {
                vo.setStatistics(getStatistics(task.getTaskId()));
            }
            return vo;
        });
    }

    /**
     * 查询任务详情（含统计）
     */
    public LiveTaskDetailVO getDetail(String taskId) {
        // 先尝试从缓存获取
        LiveTaskDetailVO cached = liveRedisCacheService.getTaskDetail(taskId);
        if (cached != null) {
            return cached;
        }

        LiveTask task = liveTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RRException(TASK_NOT_FOUND);
        }
        LiveTaskDetailVO detailVO = liveConvert.toDetailVO(task);
        // 填充统计数据
        detailVO.setStatistics(getStatistics(taskId));

        // 缓存任务详情
        liveRedisCacheService.cacheTaskDetail(taskId, detailVO);
        return detailVO;
    }

    /**
     * 创建任务
     */
    @Transactional(rollbackFor = Exception.class)
    public LiveTaskVO create(CreateLiveTaskCommand command) {
        // 检查是否已存在相同URL的PENDING或MONITORING状态任务
        LambdaQueryWrapper<LiveTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LiveTask::getRoomUrl, command.getRoomUrl())
                .in(LiveTask::getTaskStatus, TaskStatusEnum.PENDING, TaskStatusEnum.MONITORING, TaskStatusEnum.INTERRUPTED);
        Long count = liveTaskMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RRException("该直播间已在监控中，请勿重复添加");
        }

        // 验证最大时长
        validateMaxDuration(command.getMaxDuration());

        // 转换为实体
        LiveTask task = liveConvert.toEntity(command);
        task.setTaskId(leafSnowflakeGenerator.nextIdStr());
        task.setTenantId(UserUtil.getUserTenancyId());
        task.setTaskName(command.getTaskName());
        task.setCollectChat(Boolean.TRUE.equals(command.getCollectChat()));
        task.setCollectGift(Boolean.TRUE.equals(command.getCollectGift()));
        task.setCollectLike(Boolean.TRUE.equals(command.getCollectLike()));
        task.setCollectFollow(Boolean.TRUE.equals(command.getCollectFollow()));
        task.setCollectUser(Boolean.TRUE.equals(command.getCollectUser()));
        task.setCollectEcom(Boolean.TRUE.equals(command.getCollectEcom()));
        task.setMaxDuration(normalizeMaxDuration(command.getMaxDuration()));
        task.setExpireTime(new Date(System.currentTimeMillis() + task.getMaxDuration() * 60 * 1000L));
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());

        liveTaskMapper.insert(task);

        return liveConvert.toVO(task);
    }

    /**
     * 更新任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(UpdateLiveTaskCommand command) {
        LiveTask task = liveTaskMapper.selectById(command.getTaskId());
        if (task == null) {
            throw new RRException(TASK_NOT_FOUND);
        }
        if (task.getTaskStatus() == TaskStatusEnum.MONITORING) {
            throw new RRException("监控中的任务不可修改，请先停止任务");
        }
        if (task.getTaskStatus() == TaskStatusEnum.INTERRUPTED) {
            throw new RRException("异常中断的任务不可修改，请先停止任务");
        }

        LambdaQueryWrapper<LiveTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LiveTask::getRoomUrl, command.getRoomUrl())
                .ne(LiveTask::getTaskId, command.getTaskId())
                .in(LiveTask::getTaskStatus, TaskStatusEnum.PENDING, TaskStatusEnum.MONITORING, TaskStatusEnum.INTERRUPTED);
        Long count = liveTaskMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RRException("该直播间已在监控中，请勿重复添加");
        }

        validateMaxDuration(command.getMaxDuration());

        // 使用MapStruct转换器更新实体
        liveConvert.updateEntity(task, command);
        if (CharSequenceUtil.isNotBlank(command.getTaskName())) {
            task.setTaskName(command.getTaskName());
        }
        task.setCollectChat(Boolean.TRUE.equals(command.getCollectChat()));
        task.setCollectGift(Boolean.TRUE.equals(command.getCollectGift()));
        task.setCollectLike(Boolean.TRUE.equals(command.getCollectLike()));
        task.setCollectFollow(Boolean.TRUE.equals(command.getCollectFollow()));
        task.setCollectUser(Boolean.TRUE.equals(command.getCollectUser()));
        task.setMaxDuration(normalizeMaxDuration(command.getMaxDuration()));
        task.setErrorMsg(null);
        task.setUpdateTime(new Date());

        liveTaskMapper.updateById(task);

        // 清除任务相关缓存
        liveRedisCacheService.evictTaskAllCache(command.getTaskId());

        log.info("更新直播任务成功: taskId={}", command.getTaskId());
    }

    /**
     * 启动任务（分布式锁保证单实例操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public void startTask(String taskId) {
        String lockKey = TASK_LOCK_PREFIX + taskId;
        if (!redissonLock.lock(lockKey, 3, 10)) {
            throw new RRException("任务操作繁忙，请稍后重试");
        }
        try {
            LiveTask task = liveTaskMapper.selectById(taskId);
            if (task == null) {
                throw new RRException("任务不存在");
            }

            if (task.getTaskStatus() == TaskStatusEnum.MONITORING) {
                throw new RRException("任务已在监控中");
            }

            if (task.getTaskStatus() == TaskStatusEnum.INTERRUPTED) {
                // 异常中断的任务允许重新启动，清除错误信息
                task.setErrorMsg(null);
            }

            if (task.getTaskStatus() == TaskStatusEnum.EXPIRED) {
                throw new RRException("任务已过期，请重新创建");
            }

            // 更新任务状态
            task.setTaskStatus(TaskStatusEnum.MONITORING);
            task.setStartTime(new Date());
            task.setExpireTime(new Date(System.currentTimeMillis() + task.getMaxDuration() * 60 * 1000L));
            task.setErrorMsg(null);
            task.setUpdateTime(new Date());
            liveTaskMapper.updateById(task);

            // 清除任务相关缓存
            liveRedisCacheService.evictTaskAllCache(taskId);

            // 启动监控线程
            startWatching(task);
        } finally {
            redissonLock.unlock(lockKey);
        }
    }

    /**
     * 停止任务（分布式锁保证单实例操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public void stopTask(String taskId) {
        String lockKey = TASK_LOCK_PREFIX + taskId;
        if (!redissonLock.lock(lockKey, 3, 10)) {
            throw new RRException("任务操作繁忙，请稍后重试");
        }
        try {
            LiveTask task = liveTaskMapper.selectById(taskId);
            if (task == null) {
                throw new RRException("任务不存在");
            }

            if (task.getTaskStatus() != TaskStatusEnum.MONITORING
                    && task.getTaskStatus() != TaskStatusEnum.INTERRUPTED) {
                throw new RRException("任务未在监控中");
            }

            stopWatching(task, TaskStatusEnum.STOPPED, null);

            // 清除任务相关缓存
            liveRedisCacheService.evictTaskAllCache(taskId);
        } finally {
            redissonLock.unlock(lockKey);
        }
    }

    /**
     * 删除任务（分布式锁保证单实例操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(String taskId) {
        String lockKey = TASK_LOCK_PREFIX + taskId;
        if (!redissonLock.lock(lockKey, 3, 10)) {
            throw new RRException("任务操作繁忙，请稍后重试");
        }
        try {
            LiveTask task = liveTaskMapper.selectById(taskId);
            if (task == null) {
                throw new RRException("任务不存在");
            }

            // 如果正在监控或异常中断，先停止
            if (task.getTaskStatus() == TaskStatusEnum.MONITORING
                    || task.getTaskStatus() == TaskStatusEnum.INTERRUPTED) {
                stopWatching(task, TaskStatusEnum.STOPPED, null);
            }

            // 清空任务采集数据
            clearTaskDataInternal(taskId);

            liveTaskMapper.deleteById(taskId);

            // 清除任务相关缓存
            liveRedisCacheService.evictTaskAllCache(taskId);

            log.info("删除直播任务成功: taskId={}", taskId);
        } finally {
            redissonLock.unlock(lockKey);
        }
    }

    /**
     * 清空任务采集数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearTaskData(String taskId) {
        LiveTask task = liveTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RRException("任务不存在");
        }
        if (task.getTaskStatus() == TaskStatusEnum.MONITORING
                || task.getTaskStatus() == TaskStatusEnum.INTERRUPTED) {
            throw new RRException("监控中或异常中断的任务不可清空数据，请先停止任务");
        }

        clearTaskDataInternal(taskId);

        // 清除任务统计缓存和最近数据缓存
        liveRedisCacheService.evictCounters(taskId);
        liveRedisCacheService.evictRecentData(taskId);

        log.info("清空直播任务采集数据成功: taskId={}", taskId);
    }

    /**
     * 清空任务采集数据（内部方法）
     */
    private void clearTaskDataInternal(String taskId) {
        liveChatMapper.delete(new LambdaQueryWrapper<LiveChat>().eq(LiveChat::getTaskId, taskId));
        liveGiftMapper.delete(new LambdaQueryWrapper<LiveGift>().eq(LiveGift::getTaskId, taskId));
        liveLikeMapper.delete(new LambdaQueryWrapper<LiveLike>().eq(LiveLike::getTaskId, taskId));
        liveFollowMapper.delete(new LambdaQueryWrapper<LiveFollow>().eq(LiveFollow::getTaskId, taskId));
        liveUserEnterMapper.delete(new LambdaQueryWrapper<LiveUserEnter>().eq(LiveUserEnter::getTaskId, taskId));
    }

    /**
     * 查询聊天数据
     */
    public IPage<LiveChatVO> queryChat(LiveDataPageQuery query) {
        Page<LiveChat> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<LiveChat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CharSequenceUtil.isNotBlank(query.getTaskId()), LiveChat::getTaskId, query.getTaskId())
                .eq(CharSequenceUtil.isNotBlank(query.getUserId()), LiveChat::getUserId, query.getUserId())
                .like(CharSequenceUtil.isNotBlank(query.getUserNickname()), LiveChat::getUserNickname, query.getUserNickname())
                .like(CharSequenceUtil.isNotBlank(query.getContent()), LiveChat::getContent, query.getContent())
                .orderByDesc(LiveChat::getChatTime);

        IPage<LiveChat> chatPage = liveChatMapper.selectPage(page, wrapper);
        return chatPage.convert(liveConvert::toChatVO);
    }

    /**
     * 查询礼物数据
     */
    public IPage<LiveGiftVO> queryGift(LiveDataPageQuery query) {
        Page<LiveGift> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<LiveGift> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CharSequenceUtil.isNotBlank(query.getTaskId()), LiveGift::getTaskId, query.getTaskId())
                .eq(CharSequenceUtil.isNotBlank(query.getUserId()), LiveGift::getUserId, query.getUserId())
                .like(CharSequenceUtil.isNotBlank(query.getUserNickname()), LiveGift::getUserNickname, query.getUserNickname())
                .orderByDesc(LiveGift::getGiftTime);

        IPage<LiveGift> giftPage = liveGiftMapper.selectPage(page, wrapper);
        return giftPage.convert(liveConvert::toGiftVO);
    }

    /**
     * 查询点赞数据
     */
    public IPage<LiveLikeVO> queryLike(LiveDataPageQuery query) {
        Page<LiveLike> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<LiveLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CharSequenceUtil.isNotBlank(query.getTaskId()), LiveLike::getTaskId, query.getTaskId())
                .eq(CharSequenceUtil.isNotBlank(query.getUserId()), LiveLike::getUserId, query.getUserId())
                .like(CharSequenceUtil.isNotBlank(query.getUserNickname()), LiveLike::getUserNickname, query.getUserNickname())
                .orderByDesc(LiveLike::getLikeTime);

        IPage<LiveLike> likePage = liveLikeMapper.selectPage(page, wrapper);
        return likePage.convert(liveConvert::toLikeVO);
    }

    /**
     * 查询关注数据
     */
    public IPage<LiveFollowVO> queryFollow(LiveDataPageQuery query) {
        Page<LiveFollow> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<LiveFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CharSequenceUtil.isNotBlank(query.getTaskId()), LiveFollow::getTaskId, query.getTaskId())
                .eq(CharSequenceUtil.isNotBlank(query.getUserId()), LiveFollow::getUserId, query.getUserId())
                .like(CharSequenceUtil.isNotBlank(query.getUserNickname()), LiveFollow::getUserNickname, query.getUserNickname())
                .orderByDesc(LiveFollow::getFollowTime);

        IPage<LiveFollow> followPage = liveFollowMapper.selectPage(page, wrapper);
        return followPage.convert(liveConvert::toFollowVO);
    }

    /**
     * 查询用户进入数据
     */
    public IPage<LiveUserEnterVO> queryUserEnter(LiveDataPageQuery query) {
        Page<LiveUserEnter> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<LiveUserEnter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CharSequenceUtil.isNotBlank(query.getTaskId()), LiveUserEnter::getTaskId, query.getTaskId())
                .eq(CharSequenceUtil.isNotBlank(query.getUserId()), LiveUserEnter::getUserId, query.getUserId())
                .like(CharSequenceUtil.isNotBlank(query.getUserNickname()), LiveUserEnter::getUserNickname, query.getUserNickname())
                .orderByDesc(LiveUserEnter::getEnterTime);

        IPage<LiveUserEnter> enterPage = liveUserEnterMapper.selectPage(page, wrapper);
        return enterPage.convert(liveConvert::toUserEnterVO);
    }

    /**
     * 查询电商数据
     */
    public IPage<LiveEcomDataVO> queryEcom(LiveDataPageQuery query) {
        Page<LiveEcom> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<LiveEcom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CharSequenceUtil.isNotBlank(query.getTaskId()), LiveEcom::getTaskId, query.getTaskId())
                .eq(CharSequenceUtil.isNotBlank(query.getBusinessType()), LiveEcom::getBusinessType, query.getBusinessType())
                .orderByDesc(LiveEcom::getCreateTime);
        IPage<LiveEcom> ecomPage = liveEcomMapper.selectPage(page, wrapper);
        return ecomPage.convert(this::toEcomVO);
    }

    private LiveEcomDataVO toEcomVO(LiveEcom ecom) {
        LiveEcomDataVO vo = new LiveEcomDataVO();
        vo.setEcomType(ecom.getEcomType());
        vo.setRoomId(null);
        vo.setMsgType(ecom.getMsgType());
        vo.setTimestamp(ecom.getMsgTimestamp());
        vo.setBusinessType(ecom.getBusinessType());
        vo.setRawJson(ecom.getRawJson());
        return vo;
    }

    /**
     * 查询任务统计数据（优先从 Redis Hash 读取实时计数器）
     */
    public LiveTaskStatisticsVO getStatistics(String taskId) {
        // 从 Redis Hash 读取实时计数器
        if (liveRedisCacheService.hasCounters(taskId)) {
            LiveTaskStatisticsVO stats = liveRedisCacheService.getStatisticsFromHash(taskId);
            if (stats != null) {
                return stats;
            }
        }

        // 计数器不存在时从 DB 初始化
        LiveTask task = liveTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RRException("任务不存在");
        }

        LambdaQueryWrapper<LiveChat> chatWrapper = new LambdaQueryWrapper<>();
        chatWrapper.eq(LiveChat::getTaskId, taskId);
        Long chatCount = liveChatMapper.selectCount(chatWrapper);

        LambdaQueryWrapper<LiveGift> giftWrapper = new LambdaQueryWrapper<>();
        giftWrapper.eq(LiveGift::getTaskId, taskId);
        Long giftCount = liveGiftMapper.selectCount(giftWrapper);

        LambdaQueryWrapper<LiveLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(LiveLike::getTaskId, taskId);
        Long likeCount = liveLikeMapper.selectCount(likeWrapper);

        LambdaQueryWrapper<LiveFollow> followWrapper = new LambdaQueryWrapper<>();
        followWrapper.eq(LiveFollow::getTaskId, taskId);
        Long followCount = liveFollowMapper.selectCount(followWrapper);

        LambdaQueryWrapper<LiveUserEnter> enterWrapper = new LambdaQueryWrapper<>();
        enterWrapper.eq(LiveUserEnter::getTaskId, taskId);
        Long userEnterCount = liveUserEnterMapper.selectCount(enterWrapper);

        LiveTaskStatisticsVO statistics = liveConvert.toStatisticsVO(chatCount, giftCount, likeCount, followCount, userEnterCount);

        // 初始化到 Redis Hash
        liveRedisCacheService.initCountersFromHash(taskId, statistics);
        return statistics;
    }

    /**
     * 从 Redis Hash 构建统计 VO（供 WebSocket 推送使用）
     */
    private LiveTaskStatisticsVO buildStatsFromHash(String taskId) {
        LiveTaskStatisticsVO stats = liveRedisCacheService.getStatisticsFromHash(taskId);
        if (stats == null) {
            stats = new LiveTaskStatisticsVO();
        }
        return stats;
    }

    /**
     * 查询榜单实时数据
     */
    public java.util.Map<String, String> getRankData(String taskId) {
        return liveRedisCacheService.getRankData(taskId);
    }

    /**
     * 查询直播间实时数据（在线人数+排行榜）
     */
    public LiveRoomLiveDataVO getRoomLiveData(String taskId) {
        LiveRoomLiveDataVO data = liveRedisCacheService.getRoomLiveData(taskId);
        if (data == null) {
            data = new LiveRoomLiveDataVO();
        }
        return data;
    }

    /**
     * 定时任务：自动启动 PENDING 任务（检测直播间已开播的自动开始监控）
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoStartPendingTasks() {
        LambdaQueryWrapper<LiveTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(LiveTask::getTaskStatus, TaskStatusEnum.PENDING, TaskStatusEnum.INTERRUPTED)
                .and(w -> w.isNull(LiveTask::getExpireTime).or().gt(LiveTask::getExpireTime, new Date()));
        List<LiveTask> pendingTasks = liveTaskMapper.selectList(wrapper);
        for (LiveTask task : pendingTasks) {
            if (task.getExpireTime() != null && task.getExpireTime().before(new Date())) {
                continue;
            }
            try {
                if (isRoomOnline(task.getRoomUrl())) {
                    log.info("检测到直播间已开播，自动恢复监控: taskId={}, status={}",
                            task.getTaskId(), task.getTaskStatus());
                    startTask(task.getTaskId());
                }
            } catch (Exception e) {
                log.error("自动启动监控任务失败: taskId={}", task.getTaskId(), e);
            }
        }
    }

    /**
     * 定时任务：自动停止过期任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoStopExpiredTasks() {
        LambdaQueryWrapper<LiveTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(LiveTask::getTaskStatus, TaskStatusEnum.MONITORING, TaskStatusEnum.INTERRUPTED)
                .le(LiveTask::getExpireTime, new Date());

        List<LiveTask> expiredTasks = liveTaskMapper.selectList(wrapper);

        for (LiveTask task : expiredTasks) {
            log.info("自动停止过期任务: {}", task.getTaskId());
            stopWatching(task, TaskStatusEnum.EXPIRED, "监控时长已到，自动停止");
        }
    }

    /**
     * 开始监控
     */
    private void startWatching(LiveTask task) {
        executorService.submit(() -> {
            threadMap.put(task.getTaskId(), Thread.currentThread());
            try {
                switch (task.getPlatform()) {
                    case DOUYIN:
                        startDouYinWatch(task);
                        break;
                    case TIKTOK:
                        startTikTokWatch(task);
                        break;
                    default:
                        throw new RRException("不支持的平台");
                }
            } catch (Exception e) {
                log.error("监控任务异常: {}", task.getTaskId(), e);
                // 更新任务状态
                task.setTaskStatus(TaskStatusEnum.INTERRUPTED);
                task.setStopTime(new Date());
                task.setErrorMsg(e.getMessage());
                liveTaskMapper.updateById(task);

                // 清除任务相关缓存
                liveRedisCacheService.evictTaskAllCache(task.getTaskId());
            } finally {
                threadMap.remove(task.getTaskId());
            }
        });
    }

    /**
     * 启动抖音监控
     */
    private void startDouYinWatch(LiveTask task) {
        DouYinHackLiveRoomWatcherWithStats watcher;
        try {
            watcher = new DouYinHackLiveRoomWatcherWithStats(task.getRoomUrl())
                    .setTaskId(task.getTaskId())
                    .useGzip(true);
        } catch (Exception e) {
            log.error("抖音监控初始化失败，房间地址可能无效: {}, 错误: {}", task.getRoomUrl(), e.getMessage());
            stopWatching(task, TaskStatusEnum.INTERRUPTED, "直播间地址无效或初始化失败: " + e.getMessage());
            return;
        }

        // 设置普通回调
        setupCallbacks(watcher, task);
        // 设置在线人数+排行榜+直播结束回调
        setupRoomStatsCallbacks(watcher, task);

        // 开始监控
        watcherMap.put(task.getTaskId(), watcher);
        try {
            watcher.startWatch();
            log.info("抖音监控启动成功: {}", task.getTaskId());
        } catch (Exception e) {
            log.error("抖音监控启动失败: {}, 错误: {}", task.getTaskId(), e.getMessage());
            watcherMap.remove(task.getTaskId());
            stopWatching(task, TaskStatusEnum.INTERRUPTED, "WebSocket连接失败: " + e.getMessage());
        }
    }

    /**
     * 启动TikTok监控
     */
    private void startTikTokWatch(LiveTask task) {
        TikTokHackLiveRoomWatcher watcher = new TikTokHackLiveRoomWatcher(task.getRoomUrl());

        // 设置回调
        setupCallbacks(watcher, task);

        // 开始监控
        watcherMap.put(task.getTaskId(), watcher);
        watcher.startWatch();

        log.info("TikTok监控启动成功: {}", task.getTaskId());
    }

    /**
     * 停止监控
     */
    private void stopWatching(LiveTask task, TaskStatusEnum targetStatus, String errorMsg) {
        try {
            Object watcher = watcherMap.get(task.getTaskId());
            if (watcher != null) {
                if (watcher instanceof DouYinHackLiveRoomWatcherWithStats douYinWatcherWithStats) {
                    douYinWatcherWithStats.stopWatch();
                } else if (watcher instanceof DouYinHackLiveRoomWatcher douYinHackLiveRoomWatcher) {
                    douYinHackLiveRoomWatcher.stopWatch();
                } else if (watcher instanceof TikTokHackLiveRoomWatcher tiktokhackliveroomwatcher) {
                    tiktokhackliveroomwatcher.stopWatch();
                }
                watcherMap.remove(task.getTaskId());
            }

            // 中断线程
            Thread thread = threadMap.get(task.getTaskId());
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
                threadMap.remove(task.getTaskId());
            }

            // 更新任务状态
            task.setTaskStatus(targetStatus);
            task.setStopTime(new Date());
            task.setErrorMsg(errorMsg);
            task.setUpdateTime(new Date());
            liveTaskMapper.updateById(task);

            log.info("监控任务已停止: {}", task.getTaskId());
        } catch (Exception e) {
            log.error("停止监控任务失败: {}", task.getTaskId(), e);
            throw new RRException("停止监控失败: " + e.getMessage());
        }
    }

    /**
     * 设置监控回调
     */
    private void setupCallbacks(Object watcher, LiveTask task) {
        if (watcher instanceof DouYinHackLiveRoomWatcher douYinWatcher) {
            setupDouYinCallbacks(douYinWatcher, task);
        } else if (watcher instanceof TikTokHackLiveRoomWatcher tikTokWatcher) {
            setupTikTokCallbacks(tikTokWatcher, task);
        }
    }

    /**
     * 设置抖音监控回调
     */
    private void setupDouYinCallbacks(DouYinHackLiveRoomWatcher watcher, LiveTask task) {
        if (Boolean.TRUE.equals(task.getCollectChat())) {
            watcher.onChat(chat -> {
                try {
                    LiveChat liveChat = new LiveChat();
                    liveChat.setChatId(leafSnowflakeGenerator.nextIdStr());
                    liveChat.setTaskId(task.getTaskId());
                    liveChat.setUserId(safeGetUserId(chat.user()));
                    liveChat.setUserNickname(safeGetNickname(chat.user()));
                    liveChat.setUserAvatar(safeGetAvatar(chat.user()));
                    liveChat.setContent(chat.content());
                    liveChat.setChatTime(new Date());
                    liveChat.setCreateTime(new Date());

                    eventBuffer.addChat(liveChat);
                    LiveChatVO vo = liveConvert.toChatVO(liveChat);
                    liveRedisCacheService.addRecentData(task.getTaskId(), "chat", vo);
                    liveRedisCacheService.incrementChatCount(task.getTaskId());
                    liveWebSocketPublisher.pushStatistics(task.getTaskId(), buildStatsFromHash(task.getTaskId()));
                    liveWebSocketPublisher.pushNewData(task.getTaskId(), "chat", vo);
                } catch (Exception e) {
                    log.error("处理聊天消息异常: {}", e.getMessage());
                }
            });
        }

        if (Boolean.TRUE.equals(task.getCollectGift())) {
            watcher.onGift(gift -> {
                try {
                    LiveGift liveGift = new LiveGift();
                    liveGift.setGiftId(leafSnowflakeGenerator.nextIdStr());
                    liveGift.setTaskId(task.getTaskId());
                    liveGift.setUserId(safeGetUserId(gift.user()));
                    liveGift.setUserNickname(safeGetNickname(gift.user()));
                    liveGift.setUserAvatar(safeGetAvatar(gift.user()));
                    liveGift.setGiftName(gift.name());
                    liveGift.setGiftCount(safeIntCount(gift.count()));
                    liveGift.setGiftTime(new Date());
                    liveGift.setCreateTime(new Date());

                    eventBuffer.addGift(liveGift);
                    LiveGiftVO vo = liveConvert.toGiftVO(liveGift);
                    liveRedisCacheService.addRecentData(task.getTaskId(), "gift", vo);
                    liveRedisCacheService.incrementGiftCount(task.getTaskId());
                    liveWebSocketPublisher.pushStatistics(task.getTaskId(), buildStatsFromHash(task.getTaskId()));
                    liveWebSocketPublisher.pushNewData(task.getTaskId(), "gift", vo);
                } catch (Exception e) {
                    log.error("处理礼物消息异常: {}", e.getMessage(), e);
                }
            });
        }

        if (Boolean.TRUE.equals(task.getCollectLike())) {
            watcher.onLike(like -> {
                try {
                    LiveLike liveLike = new LiveLike();
                    liveLike.setLikeId(leafSnowflakeGenerator.nextIdStr());
                    liveLike.setTaskId(task.getTaskId());
                    liveLike.setUserId(safeGetUserId(like.user()));
                    liveLike.setUserNickname(safeGetNickname(like.user()));
                    liveLike.setUserAvatar(safeGetAvatar(like.user()));
                    liveLike.setLikeCount(safeIntCount(like.count()));
                    liveLike.setLikeTime(new Date());
                    liveLike.setCreateTime(new Date());

                    eventBuffer.addLike(liveLike);
                    LiveLikeVO vo = liveConvert.toLikeVO(liveLike);
                    liveRedisCacheService.addRecentData(task.getTaskId(), "like", vo);
                    liveRedisCacheService.incrementLikeCount(task.getTaskId(), safeIntCount(like.count()));
                    liveWebSocketPublisher.pushStatistics(task.getTaskId(), buildStatsFromHash(task.getTaskId()));
                    liveWebSocketPublisher.pushNewData(task.getTaskId(), "like", vo);
                } catch (Exception e) {
                    log.error("处理点赞消息异常: {}", e.getMessage());
                }
            });
        }

        if (Boolean.TRUE.equals(task.getCollectFollow())) {
            watcher.onFollow(follow -> {
                try {
                    LiveFollow liveFollow = new LiveFollow();
                    liveFollow.setFollowId(leafSnowflakeGenerator.nextIdStr());
                    liveFollow.setTaskId(task.getTaskId());
                    liveFollow.setUserId(safeGetUserId(follow.user()));
                    liveFollow.setUserNickname(safeGetNickname(follow.user()));
                    liveFollow.setUserAvatar(safeGetAvatar(follow.user()));
                    liveFollow.setFollowTime(new Date());
                    liveFollow.setCreateTime(new Date());

                    eventBuffer.addFollow(liveFollow);
                    LiveFollowVO vo = liveConvert.toFollowVO(liveFollow);
                    liveRedisCacheService.addRecentData(task.getTaskId(), "follow", vo);
                    liveRedisCacheService.incrementFollowCount(task.getTaskId());
                    liveWebSocketPublisher.pushStatistics(task.getTaskId(), buildStatsFromHash(task.getTaskId()));
                    liveWebSocketPublisher.pushNewData(task.getTaskId(), "follow", vo);
                } catch (Exception e) {
                    log.error("处理关注消息异常: {}", e.getMessage());
                }
            });
        }

        if (Boolean.TRUE.equals(task.getCollectUser())) {
            watcher.onUser(user -> {
                try {
                    LiveUserEnter liveUserEnter = new LiveUserEnter();
                    liveUserEnter.setEnterId(leafSnowflakeGenerator.nextIdStr());
                    liveUserEnter.setTaskId(task.getTaskId());
                    liveUserEnter.setUserId(safeGetUserId(user));
                    liveUserEnter.setUserNickname(safeGetNickname(user));
                    liveUserEnter.setUserAvatar(safeGetAvatar(user));
                    liveUserEnter.setEnterTime(new Date());
                    liveUserEnter.setCreateTime(new Date());

                    eventBuffer.addUserEnter(liveUserEnter);
                    LiveUserEnterVO vo = liveConvert.toUserEnterVO(liveUserEnter);
                    liveRedisCacheService.addRecentData(task.getTaskId(), "userEnter", vo);
                    liveRedisCacheService.incrementUserEnterCount(task.getTaskId());
                    liveWebSocketPublisher.pushStatistics(task.getTaskId(), buildStatsFromHash(task.getTaskId()));
                    liveWebSocketPublisher.pushNewData(task.getTaskId(), "userEnter", vo);
                } catch (Exception e) {
                    log.error("处理用户进入消息异常: {}", e.getMessage());
                }
            });
        }

        // 榜单数据回调（仅 Redis + WebSocket 实时推送，不存 DB）
        if (watcher instanceof DouYinHackLiveRoomWatcherWithStats wStats) {
            wStats.onRankData(rank -> {
                try {
                    liveRedisCacheService.cacheRankData(task.getTaskId(), rank);
                    liveWebSocketPublisher.pushNewData(task.getTaskId(), "rankData", rank);
                    log.info("榜单实时数据: taskId={}, rawJson={}", task.getTaskId(), rank.getRawJson());
                } catch (Exception ex) {
                    log.error("处理榜单数据异常: {}", ex.getMessage());
                }
            });
        }

        // 电商数据回调（可配置采集开关，各子类型独立计数，仅 RANK 以外类型存库）
        if (watcher instanceof DouYinHackLiveRoomWatcherWithStats wStats) {
            wStats.onEcomData(ecom -> {
                if (Boolean.FALSE.equals(task.getCollectEcom())) return;
                // 榜单数据已在 onRankData 处理，此处跳过
                if ("RANK".equals(ecom.getEcomType())) return;
                try {
                    String bizType = resolveBusinessType(ecom.getEcomType(), ecom.getRawJson());
                    ecom.setBusinessType(bizType);

                    // 构建 LiveEcom 实体写入 buffer，后续批量入库
                    LiveEcom liveEcom = new LiveEcom();
                    liveEcom.setEcomId(leafSnowflakeGenerator.nextIdStr());
                    liveEcom.setTaskId(task.getTaskId());
                    liveEcom.setEcomType(ecom.getEcomType());
                    liveEcom.setMsgType(ecom.getMsgType());
                    liveEcom.setBusinessType(bizType);
                    liveEcom.setRawJson(ecom.getRawJson());
                    liveEcom.setMsgTimestamp(ecom.getTimestamp());
                    liveEcom.setCreateTime(new Date());
                    eventBuffer.addEcom(liveEcom);

                    liveRedisCacheService.addRecentData(task.getTaskId(), "ecom", ecom);
                    liveRedisCacheService.incrementEcomSubCount(task.getTaskId(), bizType);
                    LiveTaskStatisticsVO stats = buildStatsFromHash(task.getTaskId());
                    liveWebSocketPublisher.pushStatistics(task.getTaskId(), stats);
                    liveWebSocketPublisher.pushNewData(task.getTaskId(), "ecom", ecom);
                    log.info("电商数据: taskId={}, ecomType={}, bizType={}, orderCount={}",
                            task.getTaskId(), ecom.getEcomType(), bizType,
                            stats.getEcomOrderCount());
                } catch (Exception ex) {
                    log.error("处理电商数据异常: {}", ex.getMessage());
                }
            });
        }
    }

    /**
     * 设置抖音在线人数+排行榜+直播结束回调
     */
    private void setupRoomStatsCallbacks(DouYinHackLiveRoomWatcherWithStats watcher, LiveTask task) {
        // 直播结束自动停止
        watcher.onStreamEnd(tid -> {
            log.info("检测到直播已结束，自动停止任务: taskId={}", tid);
            try {
                LiveTask t = liveTaskMapper.selectById(tid);
                if (t != null && t.getTaskStatus() == TaskStatusEnum.MONITORING) {
                    stopWatching(t, TaskStatusEnum.STOPPED, "直播已结束，自动停止");
                    liveRedisCacheService.evictTaskAllCache(tid);
                }
            } catch (Exception e) {
                log.error("自动停止任务失败: taskId={}", tid, e);
            }
        });

        watcher.onRoomStatsChange(stats -> {
            try {
                liveRedisCacheService.updateRoomOnlineData(
                        task.getTaskId(), stats.getOnlineDisplay(), stats.getOnlineTotal(), stats.getOnlineUpdateTime());
                LiveRoomLiveDataVO vo = liveRedisCacheService.getRoomLiveData(task.getTaskId());
                if (vo != null) {
                    liveWebSocketPublisher.pushLiveData(task.getTaskId(), vo);
                }
                log.debug("更新在线人数: taskId={}, online={}", task.getTaskId(), stats.getOnlineDisplay());
            } catch (Exception e) {
                log.error("处理在线人数失败: taskId={}", task.getTaskId(), e);
            }
        });

        watcher.onRoomRankChange(ranks -> {
            try {
                liveRedisCacheService.updateRoomRankData(task.getTaskId(), ranks, System.currentTimeMillis());
                LiveRoomLiveDataVO vo = liveRedisCacheService.getRoomLiveData(task.getTaskId());
                if (vo != null) {
                    liveWebSocketPublisher.pushLiveData(task.getTaskId(), vo);
                }
                log.debug("更新排行榜: taskId={}, rankCount={}", task.getTaskId(), ranks.size());
            } catch (Exception e) {
                log.error("处理排行榜失败: taskId={}", task.getTaskId(), e);
            }
        });
    }

    /**
     * 设置TikTok监控回调
     */
    private void setupTikTokCallbacks(TikTokHackLiveRoomWatcher watcher, LiveTask task) {
        if (Boolean.TRUE.equals(task.getCollectChat())) {
            watcher.onChat(chat -> {
                try {
                    LiveChat liveChat = new LiveChat();
                    liveChat.setChatId(leafSnowflakeGenerator.nextIdStr());
                    liveChat.setTaskId(task.getTaskId());
                    liveChat.setUserId(safeGetUserId(chat.user()));
                    liveChat.setUserNickname(safeGetNickname(chat.user()));
                    liveChat.setUserAvatar(safeGetAvatar(chat.user()));
                    liveChat.setContent(chat.content());
                    liveChat.setChatTime(new Date());
                    liveChat.setCreateTime(new Date());

                    eventBuffer.addChat(liveChat);
                    LiveChatVO vo = liveConvert.toChatVO(liveChat);
                    liveRedisCacheService.addRecentData(task.getTaskId(), "chat", vo);
                    liveRedisCacheService.incrementChatCount(task.getTaskId());
                    liveWebSocketPublisher.pushStatistics(task.getTaskId(), buildStatsFromHash(task.getTaskId()));
                    liveWebSocketPublisher.pushNewData(task.getTaskId(), "chat", vo);
                } catch (Exception e) {
                    log.error("处理TikTok聊天消息异常: {}", e.getMessage());
                }
            });
        }

        if (Boolean.TRUE.equals(task.getCollectGift())) {
            watcher.onGift(gift -> {
                try {
                    LiveGift liveGift = new LiveGift();
                    liveGift.setGiftId(leafSnowflakeGenerator.nextIdStr());
                    liveGift.setTaskId(task.getTaskId());
                    liveGift.setUserId(safeGetUserId(gift.user()));
                    liveGift.setUserNickname(safeGetNickname(gift.user()));
                    liveGift.setUserAvatar(safeGetAvatar(gift.user()));
                    liveGift.setGiftName(gift.name());
                    liveGift.setGiftCount(safeIntCount(gift.count()));
                    liveGift.setGiftTime(new Date());
                    liveGift.setCreateTime(new Date());

                    eventBuffer.addGift(liveGift);
                    LiveGiftVO vo = liveConvert.toGiftVO(liveGift);
                    liveRedisCacheService.addRecentData(task.getTaskId(), "gift", vo);
                    liveRedisCacheService.incrementGiftCount(task.getTaskId());
                    liveWebSocketPublisher.pushStatistics(task.getTaskId(), buildStatsFromHash(task.getTaskId()));
                    liveWebSocketPublisher.pushNewData(task.getTaskId(), "gift", vo);
                } catch (Exception e) {
                    log.error("处理TikTok礼物消息异常: {}", e.getMessage(), e);
                }
            });
        }

        if (Boolean.TRUE.equals(task.getCollectLike())) {
            watcher.onLike(like -> {
                try {
                    LiveLike liveLike = new LiveLike();
                    liveLike.setLikeId(leafSnowflakeGenerator.nextIdStr());
                    liveLike.setTaskId(task.getTaskId());
                    liveLike.setUserId(safeGetUserId(like.user()));
                    liveLike.setUserNickname(safeGetNickname(like.user()));
                    liveLike.setUserAvatar(safeGetAvatar(like.user()));
                    liveLike.setLikeCount(safeIntCount(like.count()));
                    liveLike.setLikeTime(new Date());
                    liveLike.setCreateTime(new Date());

                    eventBuffer.addLike(liveLike);
                    LiveLikeVO vo = liveConvert.toLikeVO(liveLike);
                    liveRedisCacheService.addRecentData(task.getTaskId(), "like", vo);
                    liveRedisCacheService.incrementLikeCount(task.getTaskId(), safeIntCount(like.count()));
                    liveWebSocketPublisher.pushStatistics(task.getTaskId(), buildStatsFromHash(task.getTaskId()));
                    liveWebSocketPublisher.pushNewData(task.getTaskId(), "like", vo);
                } catch (Exception e) {
                    log.error("处理TikTok点赞消息异常: {}", e.getMessage());
                }
            });
        }

        if (Boolean.TRUE.equals(task.getCollectFollow())) {
            watcher.onFollow(follow -> {
                try {
                    LiveFollow liveFollow = new LiveFollow();
                    liveFollow.setFollowId(leafSnowflakeGenerator.nextIdStr());
                    liveFollow.setTaskId(task.getTaskId());
                    liveFollow.setUserId(safeGetUserId(follow.user()));
                    liveFollow.setUserNickname(safeGetNickname(follow.user()));
                    liveFollow.setUserAvatar(safeGetAvatar(follow.user()));
                    liveFollow.setFollowTime(new Date());
                    liveFollow.setCreateTime(new Date());

                    eventBuffer.addFollow(liveFollow);
                    LiveFollowVO vo = liveConvert.toFollowVO(liveFollow);
                    liveRedisCacheService.addRecentData(task.getTaskId(), "follow", vo);
                    liveRedisCacheService.incrementFollowCount(task.getTaskId());
                    liveWebSocketPublisher.pushStatistics(task.getTaskId(), buildStatsFromHash(task.getTaskId()));
                    liveWebSocketPublisher.pushNewData(task.getTaskId(), "follow", vo);
                } catch (Exception e) {
                    log.error("处理TikTok关注消息异常: {}", e.getMessage());
                }
            });
        }

        if (Boolean.TRUE.equals(task.getCollectUser())) {
            watcher.onUser(user -> {
                try {
                    LiveUserEnter liveUserEnter = new LiveUserEnter();
                    liveUserEnter.setEnterId(leafSnowflakeGenerator.nextIdStr());
                    liveUserEnter.setTaskId(task.getTaskId());
                    liveUserEnter.setUserId(safeGetUserId(user));
                    liveUserEnter.setUserNickname(safeGetNickname(user));
                    liveUserEnter.setUserAvatar(safeGetAvatar(user));
                    liveUserEnter.setEnterTime(new Date());
                    liveUserEnter.setCreateTime(new Date());

                    eventBuffer.addUserEnter(liveUserEnter);
                    LiveUserEnterVO vo = liveConvert.toUserEnterVO(liveUserEnter);
                    liveRedisCacheService.addRecentData(task.getTaskId(), "userEnter", vo);
                    liveRedisCacheService.incrementUserEnterCount(task.getTaskId());
                    liveWebSocketPublisher.pushStatistics(task.getTaskId(), buildStatsFromHash(task.getTaskId()));
                    liveWebSocketPublisher.pushNewData(task.getTaskId(), "userEnter", vo);
                } catch (Exception e) {
                    log.error("处理TikTok用户进入消息异常: {}", e.getMessage());
                }
            });
        }
    }

    /**
     * 从原始 JSON 推断电商业务类型
     */
    private String resolveBusinessType(String ecomType, String rawJson) {
        // 统一按 ORDER 计数（上架/下架/促销/加购不再细分）
        return "ORDER";
    }

    /**
     * 安全获取用户ID
     */
    private String safeGetUserId(cool.scx.live_room_watcher.message.User user) {
        return user != null ? user.userID() : null;
    }

    /**
     * 安全获取用户昵称
     */
    private String safeGetNickname(cool.scx.live_room_watcher.message.User user) {
        return user != null ? user.nickname() : null;
    }

    /**
     * 安全获取用户头像
     */
    private String safeGetAvatar(cool.scx.live_room_watcher.message.User user) {
        return user != null ? user.avatar() : null;
    }

    /**
     * 安全转换礼物/点赞数量为int
     */
    private int safeIntCount(long count) {
        if (count > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (count < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) count;
    }

    /**
     * 验证最大监控时长
     */
    private void validateMaxDuration(Integer maxDuration) {
        if (maxDuration != null && maxDuration > 480) {
            throw new RRException("最大监控时长不能超过480分钟(8小时)");
        }
    }

    /**
     * 标准化最大监控时长
     */
    private Integer normalizeMaxDuration(Integer maxDuration) {
        if (maxDuration == null || maxDuration <= 0) {
            return 240;
        }
        return maxDuration;
    }

    /**
     * 应用启动时恢复所有运行中的监控任务
     */
    @PostConstruct
    public void recoverRunningTasks() {
        log.info("开始恢复运行中和异常中断的监控任务...");
        LambdaQueryWrapper<LiveTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(LiveTask::getTaskStatus, TaskStatusEnum.MONITORING, TaskStatusEnum.INTERRUPTED);
        List<LiveTask> runningTasks = liveTaskMapper.selectList(wrapper);
        if (runningTasks.isEmpty()) {
            log.info("无运行中的监控任务需要恢复");
            return;
        }
        for (LiveTask task : runningTasks) {
            try {
                log.info("恢复监控任务: taskId={}, roomUrl={}", task.getTaskId(), task.getRoomUrl());
                startWatching(task);
            } catch (Exception e) {
                log.error("恢复监控任务失败: taskId={}", task.getTaskId(), e);
            }
        }
        log.info("恢复监控任务完成: count={}", runningTasks.size());
    }

    /**
     * 检测直播间是否在线（开播中）
     */
    public boolean isRoomOnline(String roomUrl) {
        try {
            new DouYinHackLiveRoomWatcher(roomUrl).getLiveRoomInfo();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 应用关闭时，处理剩余缓冲数据
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭直播任务服务，处理剩余缓冲数据...");
        
        // 停止所有监控任务
        watcherMap.keySet().forEach(taskId -> {
            try {
                LiveTask task = liveTaskMapper.selectById(taskId);
                if (task != null && task.getTaskStatus() == TaskStatusEnum.MONITORING) {
                    stopWatching(task, TaskStatusEnum.INTERRUPTED, "服务关闭");
                }
            } catch (Exception e) {
                log.error("停止监控任务失败: taskId={}", taskId, e);
            }
        });
        
        // 处理缓冲区剩余数据
        if (eventBuffer.hasPendingData()) {
            batchProcessor.processRemainingData();
        }
        
        // 关闭线程池
        executorService.shutdown();
        
        log.info("直播任务服务已关闭");
    }
}
