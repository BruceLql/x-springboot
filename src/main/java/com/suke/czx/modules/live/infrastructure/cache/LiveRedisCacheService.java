package com.suke.czx.modules.live.infrastructure.cache;

import com.suke.czx.common.utils.Constant;
import com.suke.czx.modules.live.interfaces.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 直播模块 Redis 缓存服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveRedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String TASK_DETAIL_KEY = Constant.SYSTEM_NAME + "live:task:detail:";
    private static final String TASK_STATISTICS_KEY = Constant.SYSTEM_NAME + "live:task:statistics:";
    private static final String DATA_RECENT_KEY = Constant.SYSTEM_NAME + "live:data:recent:";
    private static final String ROOM_LIVE_DATA_KEY = Constant.SYSTEM_NAME + "live:room:liveData:";
    private static final String TASK_COUNTERS_KEY = Constant.SYSTEM_NAME + "live:task:counters:";

    private static final long TASK_DETAIL_TTL = 10;
    private static final long DATA_RECENT_TTL = 10;
    private static final int RECENT_DATA_SIZE = 100;
    private static final long ROOM_LIVE_DATA_TTL = 30;

    // ==================== 任务详情缓存 ====================

    public void cacheTaskDetail(String taskId, LiveTaskDetailVO detail) {
        if (taskId == null || detail == null) return;
        try {
            redisTemplate.opsForValue().set(TASK_DETAIL_KEY + taskId, detail, TASK_DETAIL_TTL, TimeUnit.MINUTES);
            log.debug("缓存任务详情: taskId={}", taskId);
        } catch (Exception e) {
            log.error("缓存任务详情失败: taskId={}", taskId, e);
        }
    }

    public LiveTaskDetailVO getTaskDetail(String taskId) {
        if (taskId == null) return null;
        try {
            Object value = redisTemplate.opsForValue().get(TASK_DETAIL_KEY + taskId);
            if (value instanceof LiveTaskDetailVO) {
                return (LiveTaskDetailVO) value;
            }
        } catch (Exception e) {
            log.error("获取任务详情缓存失败: taskId={}", taskId, e);
        }
        return null;
    }

    public void evictTaskDetail(String taskId) {
        if (taskId == null) return;
        try {
            redisTemplate.delete(TASK_DETAIL_KEY + taskId);
        } catch (Exception e) {
            log.error("清除任务详情缓存失败: taskId={}", taskId, e);
        }
    }

    // ==================== 最近实时数据缓存（List） ====================

    public void addRecentData(String taskId, String dataType, Object data) {
        if (taskId == null || dataType == null || data == null) return;
        try {
            String key = DATA_RECENT_KEY + taskId + ":" + dataType;
            redisTemplate.opsForList().rightPush(key, data);
            // 先push再trim：始终保留最近 RECENT_DATA_SIZE 条，避免 size→trim→push 三步的竞态
            redisTemplate.opsForList().trim(key, -RECENT_DATA_SIZE, -1);
            redisTemplate.expire(key, DATA_RECENT_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("添加最近数据缓存失败: taskId={}, type={}", taskId, dataType, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getRecentData(String taskId, String dataType, int size) {
        if (taskId == null || dataType == null) return new ArrayList<>();
        try {
            String key = DATA_RECENT_KEY + taskId + ":" + dataType;
            List<Object> list = redisTemplate.opsForList().range(key, -size, -1);
            if (list != null && !list.isEmpty()) {
                return (List<T>) list;
            }
        } catch (Exception e) {
            log.error("获取最近数据缓存失败: taskId={}, type={}", taskId, dataType, e);
        }
        return new ArrayList<>();
    }

    public void evictRecentData(String taskId) {
        if (taskId == null) return;
        try {
            String pattern = DATA_RECENT_KEY + taskId + ":*";
            var connection = redisTemplate.getConnectionFactory().getConnection();
            var keys = connection.keyCommands().keys(pattern.getBytes());
            if (keys != null) {
                for (byte[] key : keys) {
                    redisTemplate.delete(new String(key));
                }
            }
        } catch (Exception e) {
            log.error("清除最近数据缓存失败: taskId={}", taskId, e);
        }
    }

    // ==================== 统计计数器（Hash） ====================

    private static final String HASH_CHAT_COUNT = "chatCount";
    private static final String HASH_GIFT_COUNT = "giftCount";
    private static final String HASH_LIKE_COUNT = "likeCount";
    private static final String HASH_FOLLOW_COUNT = "followCount";
    private static final String HASH_USER_ENTER_COUNT = "userEnterCount";
    private static final String HASH_ECOM_ORDER = "ecomOrder";

    public void incrementChatCount(String taskId) {
        if (taskId == null) return;
        try {
            stringRedisTemplate.opsForHash().increment(TASK_COUNTERS_KEY + taskId, HASH_CHAT_COUNT, 1L);
        } catch (Exception e) {
            log.error("INCR chatCount 失败: taskId={}", taskId, e);
        }
    }

    public void incrementGiftCount(String taskId) {
        if (taskId == null) return;
        try {
            stringRedisTemplate.opsForHash().increment(TASK_COUNTERS_KEY + taskId, HASH_GIFT_COUNT, 1L);
        } catch (Exception e) {
            log.error("INCR giftCount 失败: taskId={}", taskId, e);
        }
    }

    public void incrementLikeCount(String taskId, long count) {
        if (taskId == null) return;
        try {
            stringRedisTemplate.opsForHash().increment(TASK_COUNTERS_KEY + taskId, HASH_LIKE_COUNT, count);
        } catch (Exception e) {
            log.error("INCR likeCount 失败: taskId={}", taskId, e);
        }
    }

    public void incrementFollowCount(String taskId) {
        if (taskId == null) return;
        try {
            stringRedisTemplate.opsForHash().increment(TASK_COUNTERS_KEY + taskId, HASH_FOLLOW_COUNT, 1L);
        } catch (Exception e) {
            log.error("INCR followCount 失败: taskId={}", taskId, e);
        }
    }

    public void incrementUserEnterCount(String taskId) {
        if (taskId == null) return;
        try {
            stringRedisTemplate.opsForHash().increment(TASK_COUNTERS_KEY + taskId, HASH_USER_ENTER_COUNT, 1L);
        } catch (Exception e) {
            log.error("INCR userEnterCount 失败: taskId={}", taskId, e);
        }
    }

    public void incrementEcomSubCount(String taskId, String businessType) {
        if (taskId == null || businessType == null) return;
        String hashKey = "ORDER".equals(businessType) ? HASH_ECOM_ORDER : HASH_ECOM_ORDER; // 统一用 ecomOrder 计数
        try {
            stringRedisTemplate.opsForHash().increment(TASK_COUNTERS_KEY + taskId, hashKey, 1L);
        } catch (Exception e) {
            log.error("INCR ecomOrder 失败: taskId={}", taskId, e);
        }
    }

    /**
     * 从 Redis Hash 读取统计数据计数器（使用 StringRedisTemplate，值与 HINCRBY 兼容）
     */
    public LiveTaskStatisticsVO getStatisticsFromHash(String taskId) {
        if (taskId == null) return null;
        try {
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(TASK_COUNTERS_KEY + taskId);
            if (entries.isEmpty()) return null;
            LiveTaskStatisticsVO vo = new LiveTaskStatisticsVO();
            vo.setChatCount(toLong(entries.get(HASH_CHAT_COUNT)));
            vo.setGiftCount(toLong(entries.get(HASH_GIFT_COUNT)));
            vo.setLikeCount(toLong(entries.get(HASH_LIKE_COUNT)));
            vo.setFollowCount(toLong(entries.get(HASH_FOLLOW_COUNT)));
            vo.setUserEnterCount(toLong(entries.get(HASH_USER_ENTER_COUNT)));
            vo.setEcomOrderCount(toLong(entries.get(HASH_ECOM_ORDER)));
            return vo;
        } catch (Exception e) {
            log.error("获取统计数据 Hash 失败: taskId={}", taskId, e);
        }
        return null;
    }

    /**
     * 从 DB COUNT 结果初始化计数器 Hash（使用 putIfAbsent 避免覆盖回调线程已 HINCRBY 的值）
     */
    public void initCountersFromHash(String taskId, LiveTaskStatisticsVO statistics) {
        if (taskId == null || statistics == null) return;
        try {
            String key = TASK_COUNTERS_KEY + taskId;
            stringRedisTemplate.opsForHash().putIfAbsent(key, HASH_CHAT_COUNT, String.valueOf(statistics.getChatCount() != null ? statistics.getChatCount() : 0L));
            stringRedisTemplate.opsForHash().putIfAbsent(key, HASH_GIFT_COUNT, String.valueOf(statistics.getGiftCount() != null ? statistics.getGiftCount() : 0L));
            stringRedisTemplate.opsForHash().putIfAbsent(key, HASH_LIKE_COUNT, String.valueOf(statistics.getLikeCount() != null ? statistics.getLikeCount() : 0L));
            stringRedisTemplate.opsForHash().putIfAbsent(key, HASH_FOLLOW_COUNT, String.valueOf(statistics.getFollowCount() != null ? statistics.getFollowCount() : 0L));
            stringRedisTemplate.opsForHash().putIfAbsent(key, HASH_USER_ENTER_COUNT, String.valueOf(statistics.getUserEnterCount() != null ? statistics.getUserEnterCount() : 0L));
            stringRedisTemplate.opsForHash().putIfAbsent(key, HASH_ECOM_ORDER, "0");
            log.debug("初始化计数器 Hash: taskId={}", taskId);
        } catch (Exception e) {
            log.error("初始化计数器 Hash 失败: taskId={}", taskId, e);
        }
    }

    public boolean hasCounters(String taskId) {
        if (taskId == null) return false;
        try {
            Boolean hasKey = stringRedisTemplate.hasKey(TASK_COUNTERS_KEY + taskId);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            return false;
        }
    }

    public void evictCounters(String taskId) {
        if (taskId == null) return;
        try {
            stringRedisTemplate.delete(TASK_COUNTERS_KEY + taskId);
        } catch (Exception e) {
            log.error("清除计数器缓存失败: taskId={}", taskId, e);
        }
    }

    // ==================== 直播间实时数据（Hash，原子更新，消除竞态） ====================

    private static final String HASH_ONLINE_DISPLAY = "onlineDisplay";
    private static final String HASH_ONLINE_TOTAL = "onlineTotal";
    private static final String HASH_ONLINE_UPDATE_TIME = "onlineUpdateTime";
    private static final String HASH_RANKS = "ranks";
    private static final String HASH_RANK_UPDATE_TIME = "rankUpdateTime";

    /**
     * 原子更新在线人数数据（仅更新在线人数字段，不影响排行榜字段）
     */
    public void updateRoomOnlineData(String taskId, String onlineDisplay, Long onlineTotal, Long onlineUpdateTime) {
        if (taskId == null) return;
        try {
            String key = ROOM_LIVE_DATA_KEY + taskId;
            redisTemplate.opsForHash().put(key, HASH_ONLINE_DISPLAY, onlineDisplay != null ? onlineDisplay : "");
            redisTemplate.opsForHash().put(key, HASH_ONLINE_TOTAL, onlineTotal != null ? onlineTotal : 0L);
            redisTemplate.opsForHash().put(key, HASH_ONLINE_UPDATE_TIME, onlineUpdateTime);
            redisTemplate.expire(key, ROOM_LIVE_DATA_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("更新在线人数数据失败: taskId={}", taskId, e);
        }
    }

    /**
     * 原子更新排行榜数据（仅更新排行榜字段，不影响在线人数字段）
     */
    public void updateRoomRankData(String taskId, List<LiveRoomLiveDataVO.RankItem> ranks, Long rankUpdateTime) {
        if (taskId == null) return;
        try {
            String key = ROOM_LIVE_DATA_KEY + taskId;
            redisTemplate.opsForHash().put(key, HASH_RANKS, ranks != null ? new ArrayList<>(ranks) : new ArrayList<>());
            redisTemplate.opsForHash().put(key, HASH_RANK_UPDATE_TIME, rankUpdateTime);
            redisTemplate.expire(key, ROOM_LIVE_DATA_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("更新排行榜数据失败: taskId={}", taskId, e);
        }
    }

    /**
     * 获取直播间实时数据（从 Hash 读取）
     */
    @SuppressWarnings("unchecked")
    public LiveRoomLiveDataVO getRoomLiveData(String taskId) {
        if (taskId == null) return null;
        try {
            String key = ROOM_LIVE_DATA_KEY + taskId;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries.isEmpty()) return null;

            LiveRoomLiveDataVO vo = new LiveRoomLiveDataVO();
            Object display = entries.get(HASH_ONLINE_DISPLAY);
            vo.setOnlineDisplay(display != null ? display.toString() : null);
            Object total = entries.get(HASH_ONLINE_TOTAL);
            vo.setOnlineTotal(total instanceof Number ? ((Number) total).longValue() : null);
            Object onlineTime = entries.get(HASH_ONLINE_UPDATE_TIME);
            vo.setOnlineUpdateTime(onlineTime instanceof Number ? ((Number) onlineTime).longValue() : null);
            Object ranksObj = entries.get(HASH_RANKS);
            if (ranksObj instanceof List) {
                vo.setRanks((List<LiveRoomLiveDataVO.RankItem>) ranksObj);
            }
            Object rankTime = entries.get(HASH_RANK_UPDATE_TIME);
            vo.setRankUpdateTime(rankTime instanceof Number ? ((Number) rankTime).longValue() : null);
            return vo;
        } catch (Exception e) {
            log.error("获取直播间实时数据失败: taskId={}", taskId, e);
        }
        return null;
    }

    // ==================== 电商榜单实时数据（Hash，不存 DB） ====================

    private static final String RANK_DATA_KEY = Constant.SYSTEM_NAME + "live:rank:data:";

    public void cacheRankData(String taskId, LiveEcomDataVO rank) {
        if (taskId == null || rank == null) return;
        try {
            String key = RANK_DATA_KEY + taskId;
            redisTemplate.opsForHash().put(key, "rankName", rank.getRawJson());
            redisTemplate.opsForHash().put(key, "msgType", rank.getMsgType());
            redisTemplate.opsForHash().put(key, "updateTime", String.valueOf(rank.getTimestamp()));
            redisTemplate.expire(key, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存榜单数据失败: taskId={}", taskId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getRankData(String taskId) {
        if (taskId == null) return null;
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(RANK_DATA_KEY + taskId);
            if (entries.isEmpty()) return null;
            Map<String, String> result = new java.util.HashMap<>();
            entries.forEach((k, v) -> result.put(k.toString(), v != null ? v.toString() : null));
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public void evictRoomLiveData(String taskId) {
        if (taskId == null) return;
        try {
            redisTemplate.delete(ROOM_LIVE_DATA_KEY + taskId);
        } catch (Exception e) {
            log.error("清除直播间实时数据缓存失败: taskId={}", taskId, e);
        }
    }

    // ==================== 批量清除 ====================

    public void evictTaskAllCache(String taskId) {
        if (taskId == null) return;
        evictTaskDetail(taskId);
        evictRecentData(taskId);
        evictCounters(taskId);
        evictRoomLiveData(taskId);
        log.info("清除任务所有缓存: taskId={}", taskId);
    }

    // ==================== 兼容旧接口 ====================

    /** @deprecated 改用 getStatisticsFromHash */
    @Deprecated
    public void cacheTaskStatistics(String taskId, LiveTaskStatisticsVO statistics) {}

    /** @deprecated 改用 getStatisticsFromHash */
    @Deprecated
    public LiveTaskStatisticsVO getTaskStatistics(String taskId) {
        return getStatisticsFromHash(taskId);
    }

    /** @deprecated 改用 evictCounters */
    @Deprecated
    public void evictTaskStatistics(String taskId) {
        evictCounters(taskId);
    }

    private static Long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
