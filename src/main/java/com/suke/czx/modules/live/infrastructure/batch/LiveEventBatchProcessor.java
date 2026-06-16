package com.suke.czx.modules.live.infrastructure.batch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.suke.czx.modules.live.domain.entity.*;
import com.suke.czx.modules.live.infrastructure.buffer.LiveEventBuffer;
import com.suke.czx.modules.live.infrastructure.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 直播数据批量处理器
 * 定时从缓冲区获取数据并使用 MyBatis Batch Executor 批量写入数据库
 */
@Slf4j
@Component
public class LiveEventBatchProcessor {

    private final LiveEventBuffer eventBuffer;
    private final LiveChatMapper liveChatMapper;
    private final LiveGiftMapper liveGiftMapper;
    private final LiveLikeMapper liveLikeMapper;
    private final LiveFollowMapper liveFollowMapper;
    private final LiveUserEnterMapper liveUserEnterMapper;
    private final LiveEcomMapper liveEcomMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public LiveEventBatchProcessor(LiveEventBuffer eventBuffer,
                                   LiveChatMapper liveChatMapper,
                                   LiveGiftMapper liveGiftMapper,
                                   LiveLikeMapper liveLikeMapper,
                                   LiveFollowMapper liveFollowMapper,
                                   LiveUserEnterMapper liveUserEnterMapper,
                                   LiveEcomMapper liveEcomMapper,
                                   SqlSessionFactory sqlSessionFactory) {
        this.eventBuffer = eventBuffer;
        this.liveChatMapper = liveChatMapper;
        this.liveGiftMapper = liveGiftMapper;
        this.liveLikeMapper = liveLikeMapper;
        this.liveFollowMapper = liveFollowMapper;
        this.liveUserEnterMapper = liveUserEnterMapper;
        this.liveEcomMapper = liveEcomMapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    private static final int BATCH_SIZE = 100;

    /**
     * 定时批量处理聊天数据（每1.5秒执行一次）
     */
    @Scheduled(fixedDelay = 1500)
    public void processChatBatch() {
        List<LiveChat> batch = eventBuffer.drainChatBatch(BATCH_SIZE);
        if (!batch.isEmpty()) {
            batchInsertChat(batch);
        }
    }

    /**
     * 定时批量处理礼物数据（每1.5秒执行一次）
     */
    @Scheduled(fixedDelay = 1500)
    public void processGiftBatch() {
        List<LiveGift> batch = eventBuffer.drainGiftBatch(BATCH_SIZE);
        if (!batch.isEmpty()) {
            batchInsertGift(batch);
        }
    }

    /**
     * 定时批量处理点赞数据（每1.5秒执行一次）
     */
    @Scheduled(fixedDelay = 1500)
    public void processLikeBatch() {
        List<LiveLike> batch = eventBuffer.drainLikeBatch(BATCH_SIZE * 5);
        if (!batch.isEmpty()) {
            batchInsertLike(batch);
        }
    }

    /**
     * 定时批量处理关注数据（每1.5秒执行一次）
     */
    @Scheduled(fixedDelay = 1500)
    public void processFollowBatch() {
        List<LiveFollow> batch = eventBuffer.drainFollowBatch(BATCH_SIZE);
        if (!batch.isEmpty()) {
            batchInsertFollow(batch);
        }
    }

    /**
     * 定时批量处理用户进入数据（每1.5秒执行一次）
     */
    @Scheduled(fixedDelay = 1500)
    public void processUserEnterBatch() {
        List<LiveUserEnter> batch = eventBuffer.drainUserEnterBatch(BATCH_SIZE);
        if (!batch.isEmpty()) {
            batchInsertUserEnter(batch);
        }
    }

    private void batchInsertChat(List<LiveChat> batch) {
        long startTime = System.currentTimeMillis();
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            LiveChatMapper mapper = sqlSession.getMapper(LiveChatMapper.class);
            for (LiveChat chat : batch) {
                mapper.insert(chat);
            }
            sqlSession.commit();
            log.info("批量插入聊天数据: count={}, time={}ms", batch.size(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("批量处理聊天数据失败", e);
        }
    }

    private void batchInsertGift(List<LiveGift> batch) {
        long startTime = System.currentTimeMillis();
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            LiveGiftMapper mapper = sqlSession.getMapper(LiveGiftMapper.class);
            for (LiveGift gift : batch) {
                mapper.insert(gift);
            }
            sqlSession.commit();
            log.info("批量插入礼物数据: count={}, time={}ms", batch.size(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("批量处理礼物数据失败", e);
        }
    }

    private void batchInsertLike(List<LiveLike> batch) {
        long startTime = System.currentTimeMillis();
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            LiveLikeMapper mapper = sqlSession.getMapper(LiveLikeMapper.class);
            for (LiveLike like : batch) {
                mapper.insert(like);
            }
            sqlSession.commit();
            log.info("批量插入点赞数据: count={}, time={}ms", batch.size(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("批量处理点赞数据失败", e);
        }
    }

    private void batchInsertFollow(List<LiveFollow> batch) {
        long startTime = System.currentTimeMillis();
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            LiveFollowMapper mapper = sqlSession.getMapper(LiveFollowMapper.class);
            for (LiveFollow follow : batch) {
                mapper.insert(follow);
            }
            sqlSession.commit();
            log.info("批量插入关注数据: count={}, time={}ms", batch.size(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("批量处理关注数据失败", e);
        }
    }

    private void batchInsertUserEnter(List<LiveUserEnter> batch) {
        long startTime = System.currentTimeMillis();
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            LiveUserEnterMapper mapper = sqlSession.getMapper(LiveUserEnterMapper.class);
            for (LiveUserEnter userEnter : batch) {
                mapper.insert(userEnter);
            }
            sqlSession.commit();
            log.info("批量插入用户进入数据: count={}, time={}ms", batch.size(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("批量处理用户进入数据失败", e);
        }
    }

    /**
     * 应用关闭时，处理剩余数据（逐条插入，不使用批量模式以保证事务完整性）
     */
    @Transactional(rollbackFor = Exception.class)
    public void processRemainingData() {
        log.info("开始处理剩余事件数据...");

        int remainingCount = 0;

        List<LiveChat> chatBatch = eventBuffer.drainChatBatch(Integer.MAX_VALUE);
        for (LiveChat chat : chatBatch) {
            liveChatMapper.insert(chat);
            remainingCount++;
        }

        List<LiveGift> giftBatch = eventBuffer.drainGiftBatch(Integer.MAX_VALUE);
        for (LiveGift gift : giftBatch) {
            liveGiftMapper.insert(gift);
            remainingCount++;
        }

        List<LiveLike> likeBatch = eventBuffer.drainLikeBatch(Integer.MAX_VALUE);
        for (LiveLike like : likeBatch) {
            liveLikeMapper.insert(like);
            remainingCount++;
        }

        List<LiveFollow> followBatch = eventBuffer.drainFollowBatch(Integer.MAX_VALUE);
        for (LiveFollow follow : followBatch) {
            liveFollowMapper.insert(follow);
            remainingCount++;
        }

        List<LiveUserEnter> userEnterBatch = eventBuffer.drainUserEnterBatch(Integer.MAX_VALUE);
        for (LiveUserEnter userEnter : userEnterBatch) {
            liveUserEnterMapper.insert(userEnter);
            remainingCount++;
        }

        List<LiveEcom> ecomBatch = eventBuffer.drainEcomBatch(Integer.MAX_VALUE);
        for (LiveEcom ecom : ecomBatch) {
            liveEcomMapper.insert(ecom);
            remainingCount++;
        }

        if (remainingCount > 0) {
            log.info("处理剩余事件数据完成: count={}", remainingCount);
        }
    }

    @Scheduled(fixedDelay = 1500)
    public void processEcomBatch() {
        List<LiveEcom> batch = eventBuffer.drainEcomBatch(BATCH_SIZE);
        if (!batch.isEmpty()) {
            batchInsertEcom(batch);
        }
    }

    private void batchInsertEcom(List<LiveEcom> batch) {
        long startTime = System.currentTimeMillis();
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            LiveEcomMapper mapper = sqlSession.getMapper(LiveEcomMapper.class);
            for (LiveEcom ecom : batch) {
                mapper.insert(ecom);
            }
            sqlSession.commit();
            log.info("批量插入电商数据: count={}, time={}ms", batch.size(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("批量处理电商数据失败", e);
        }
    }
}
