package com.suke.czx.modules.live.infrastructure.buffer;

import com.suke.czx.modules.live.domain.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 直播事件缓冲区
 * 用于高频场景下的事件缓冲和批量处理
 */
@Slf4j
@Component
public class LiveEventBuffer {

    /**
     * 队列容量上限（防止缓冲区无限增长导致OOM）
     */
    private static final int QUEUE_CAPACITY = 10_000;
    private final BlockingQueue<LiveChat> chatQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final BlockingQueue<LiveGift> giftQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final BlockingQueue<LiveLike> likeQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final BlockingQueue<LiveFollow> followQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final BlockingQueue<LiveUserEnter> userEnterQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final BlockingQueue<LiveEcom> ecomQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    /**
     * 统计计数器
     */
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalDropped = new AtomicLong(0);

    /**
     * 添加聊天事件
     *
     * @param chat 聊天数据
     * @return 是否添加成功
     */
    public boolean addChat(LiveChat chat) {
        boolean success = chatQueue.offer(chat);
        if (!success) {
            totalDropped.incrementAndGet();
            log.warn("聊天事件队列已满，丢弃数据: taskId={}", chat.getTaskId());
        }
        return success;
    }

    /**
     * 添加礼物事件
     *
     * @param gift 礼物数据
     * @return 是否添加成功
     */
    public boolean addGift(LiveGift gift) {
        boolean success = giftQueue.offer(gift);
        if (!success) {
            totalDropped.incrementAndGet();
            log.warn("礼物事件队列已满，丢弃数据: taskId={}", gift.getTaskId());
        }
        return success;
    }

    /**
     * 添加点赞事件
     *
     * @param like 点赞数据
     * @return 是否添加成功
     */
    public boolean addLike(LiveLike like) {
        boolean success = likeQueue.offer(like);
        if (!success) {
            totalDropped.incrementAndGet();
            log.warn("点赞事件队列已满，丢弃数据: taskId={}", like.getTaskId());
        }
        return success;
    }

    /**
     * 添加关注事件
     *
     * @param follow 关注数据
     * @return 是否添加成功
     */
    public boolean addFollow(LiveFollow follow) {
        boolean success = followQueue.offer(follow);
        if (!success) {
            totalDropped.incrementAndGet();
            log.warn("关注事件队列已满，丢弃数据: taskId={}", follow.getTaskId());
        }
        return success;
    }

    /**
     * 添加用户进入事件
     *
     * @param userEnter 用户进入数据
     * @return 是否添加成功
     */
    public boolean addUserEnter(LiveUserEnter userEnter) {
        boolean success = userEnterQueue.offer(userEnter);
        if (!success) {
            totalDropped.incrementAndGet();
            log.warn("用户进入事件队列已满，丢弃数据: taskId={}", userEnter.getTaskId());
        }
        return success;
    }

    public boolean addEcom(LiveEcom ecom) {
        boolean success = ecomQueue.offer(ecom);
        if (!success) {
            totalDropped.incrementAndGet();
            log.warn("电商事件队列已满，丢弃数据: taskId={}", ecom.getTaskId());
        }
        return success;
    }

    /**
     * 批量获取聊天事件
     *
     * @param maxBatchSize 最大批次大小
     * @return 事件列表
     */
    public List<LiveChat> drainChatBatch(int maxBatchSize) {
        List<LiveChat> batch = new ArrayList<>(maxBatchSize);
        chatQueue.drainTo(batch, maxBatchSize);
        totalProcessed.addAndGet(batch.size());
        return batch;
    }

    /**
     * 批量获取礼物事件
     *
     * @param maxBatchSize 最大批次大小
     * @return 事件列表
     */
    public List<LiveGift> drainGiftBatch(int maxBatchSize) {
        List<LiveGift> batch = new ArrayList<>(maxBatchSize);
        giftQueue.drainTo(batch, maxBatchSize);
        totalProcessed.addAndGet(batch.size());
        return batch;
    }

    /**
     * 批量获取点赞事件
     *
     * @param maxBatchSize 最大批次大小
     * @return 事件列表
     */
    public List<LiveLike> drainLikeBatch(int maxBatchSize) {
        List<LiveLike> batch = new ArrayList<>(maxBatchSize);
        likeQueue.drainTo(batch, maxBatchSize);
        totalProcessed.addAndGet(batch.size());
        return batch;
    }

    /**
     * 批量获取关注事件
     *
     * @param maxBatchSize 最大批次大小
     * @return 事件列表
     */
    public List<LiveFollow> drainFollowBatch(int maxBatchSize) {
        List<LiveFollow> batch = new ArrayList<>(maxBatchSize);
        followQueue.drainTo(batch, maxBatchSize);
        totalProcessed.addAndGet(batch.size());
        return batch;
    }

    /**
     * 批量获取用户进入事件
     *
     * @param maxBatchSize 最大批次大小
     * @return 事件列表
     */
    public List<LiveUserEnter> drainUserEnterBatch(int maxBatchSize) {
        List<LiveUserEnter> batch = new ArrayList<>(maxBatchSize);
        userEnterQueue.drainTo(batch, maxBatchSize);
        totalProcessed.addAndGet(batch.size());
        return batch;
    }

    public List<LiveEcom> drainEcomBatch(int maxBatchSize) {
        List<LiveEcom> batch = new ArrayList<>(maxBatchSize);
        ecomQueue.drainTo(batch, maxBatchSize);
        totalProcessed.addAndGet(batch.size());
        return batch;
    }

    /**
     * 获取队列大小
     */
    public int getChatQueueSize() {
        return chatQueue.size();
    }

    public int getGiftQueueSize() {
        return giftQueue.size();
    }

    public int getLikeQueueSize() {
        return likeQueue.size();
    }

    public int getFollowQueueSize() {
        return followQueue.size();
    }

    public int getUserEnterQueueSize() {
        return userEnterQueue.size();
    }

    public int getEcomQueueSize() {
        return ecomQueue.size();
    }

    /**
     * 获取已处理总数
     */
    public long getTotalProcessed() {
        return totalProcessed.get();
    }

    /**
     * 获取丢弃总数
     */
    public long getTotalDropped() {
        return totalDropped.get();
    }

    /**
     * 检查是否有待处理数据
     */
    public boolean hasPendingData() {
        return !chatQueue.isEmpty() || !giftQueue.isEmpty() ||
               !likeQueue.isEmpty() || !followQueue.isEmpty() ||
               !userEnterQueue.isEmpty() || !ecomQueue.isEmpty();
    }
}
