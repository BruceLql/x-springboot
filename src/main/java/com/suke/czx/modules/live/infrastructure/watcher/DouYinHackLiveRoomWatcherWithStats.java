package com.suke.czx.modules.live.infrastructure.watcher;

import com.suke.czx.modules.live.interfaces.vo.LiveEcomDataVO;
import com.suke.czx.modules.live.interfaces.vo.LiveRoomLiveDataVO;
import com.google.protobuf.InvalidProtocolBufferException;
import cool.scx.live_room_watcher.impl.douyin_hack.DouYinHackLiveRoomWatcher;
import cool.scx.live_room_watcher.impl.douyin_hack.enumeration.ControlMessageAction;
import cool.scx.live_room_watcher.impl.douyin_hack.proto_entity.webcast.data.Image;
import cool.scx.live_room_watcher.impl.douyin_hack.proto_entity.webcast.data.User;
import cool.scx.live_room_watcher.impl.douyin_hack.proto_entity.webcast.im.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 抖音监控扩展类：在线人数、房间排行榜、直播结束、电商下单数据回调
 */
@Slf4j
public class DouYinHackLiveRoomWatcherWithStats extends DouYinHackLiveRoomWatcher {

    private Consumer<LiveRoomLiveDataVO> onRoomStats;
    private Consumer<List<LiveRoomLiveDataVO.RankItem>> onRoomRank;
    private Consumer<String> onStreamEnd;
    private Consumer<LiveEcomDataVO> onEcomData;
    private Consumer<LiveEcomDataVO> onRankData;
    private String taskId;

    public DouYinHackLiveRoomWatcherWithStats(String roomUrl) {
        super(roomUrl);
    }

    public DouYinHackLiveRoomWatcherWithStats setTaskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public DouYinHackLiveRoomWatcherWithStats onRoomStatsChange(Consumer<LiveRoomLiveDataVO> handler) {
        this.onRoomStats = handler;
        return this;
    }

    public DouYinHackLiveRoomWatcherWithStats onRoomRankChange(Consumer<List<LiveRoomLiveDataVO.RankItem>> handler) {
        this.onRoomRank = handler;
        return this;
    }

    public DouYinHackLiveRoomWatcherWithStats onStreamEnd(Consumer<String> handler) {
        this.onStreamEnd = handler;
        return this;
    }

    /**
     * 电商数据回调（下单/购买/商品推广等）
     */
    public DouYinHackLiveRoomWatcherWithStats onEcomData(Consumer<LiveEcomDataVO> handler) {
        this.onEcomData = handler;
        return this;
    }

    /**
     * 榜单数据回调（仅 Redis + WebSocket 实时推送，不存 DB）
     */
    public DouYinHackLiveRoomWatcherWithStats onRankData(Consumer<LiveEcomDataVO> handler) {
        this.onRankData = handler;
        return this;
    }

    @Override
    public DouYinHackLiveRoomWatcherWithStats useGzip(boolean useGzip) {
        super.useGzip(useGzip);
        return this;
    }

    // ==================== 房间状态 ====================

    @Override
    public void WebcastRoomStatsMessage(byte[] payload) throws InvalidProtocolBufferException {
        try {
            RoomStatsMessage msg = RoomStatsMessage.parseFrom(payload);
            if (onRoomStats != null) {
                LiveRoomLiveDataVO vo = new LiveRoomLiveDataVO();
                vo.setOnlineDisplay(msg.getDisplayMiddle());
                vo.setOnlineTotal(msg.getTotal());
                vo.setOnlineUpdateTime(System.currentTimeMillis());
                onRoomStats.accept(vo);
            }
        } catch (Exception e) {
            log.warn("解析在线人数消息失败: {}", e.getMessage());
        }
        super.WebcastRoomStatsMessage(payload);
    }

    @Override
    public void WebcastRoomRankMessage(byte[] payload) throws InvalidProtocolBufferException {
        try {
            RoomRankMessage msg = RoomRankMessage.parseFrom(payload);
            if (onRoomRank != null) {
                List<LiveRoomLiveDataVO.RankItem> list = new ArrayList<>();
                List<RoomRankMessage.RoomRank> ranks = msg.getRanksList();
                for (int i = 0; i < ranks.size(); i++) {
                    RoomRankMessage.RoomRank rank = ranks.get(i);
                    LiveRoomLiveDataVO.RankItem item = new LiveRoomLiveDataVO.RankItem();
                    item.setRank(i + 1);
                    item.setScoreStr(rank.getScoreStr());
                    if (rank.hasUser()) {
                        User user = rank.getUser();
                        item.setUserId(String.valueOf(user.getId()));
                        item.setUserNickname(user.getNickname());
                        item.setUserAvatar(extractAvatar(user));
                    }
                    list.add(item);
                }
                onRoomRank.accept(list);
            }
        } catch (Exception e) {
            log.warn("解析房间排行榜消息失败: {}", e.getMessage());
        }
        super.WebcastRoomRankMessage(payload);
    }

    @Override
    public void WebcastControlMessage(byte[] payload) throws InvalidProtocolBufferException {
        try {
            ControlMessage controlMessage = ControlMessage.parseFrom(payload);
            var actionCode = controlMessage.getAction();
            var action = ControlMessageAction.of(actionCode);
            switch (action) {
                case FINISH, FINISH_BY_ADMIN, ROOM_FINISH_BY_SWITCH -> {
                    log.info("直播间直播已结束: taskId={}, action={}", taskId, action);
                    if (onStreamEnd != null) {
                        onStreamEnd.accept(taskId);
                    }
                }
                case RESUME -> log.debug("直播间恢复直播: taskId={}", taskId);
                case PAUSE -> log.debug("直播间暂停: taskId={}", taskId);
            }
        } catch (Exception e) {
            log.warn("解析控制消息失败: {}", e.getMessage());
        }
        super.WebcastControlMessage(payload);
    }

    // ==================== 电商消息 ====================

    @Override
    public void WebcastLiveShoppingMessage(byte[] payload) throws InvalidProtocolBufferException {
        try {
            LiveShoppingMessage msg = LiveShoppingMessage.parseFrom(payload);
            if (onEcomData != null) {
                LiveEcomDataVO vo = new LiveEcomDataVO();
                vo.setEcomType("SHOPPING");
                vo.setRoomId(msg.getRoomId());
                vo.setMsgType(msg.getMsgType());
                vo.setTimestamp(msg.getTimestamp());
                vo.setRawJson(safeBytesToString(msg.getContent()));
                onEcomData.accept(vo);
            }
        } catch (Exception e) {
            log.warn("解析购物消息失败: taskId={}", taskId, e);
        }
        super.WebcastLiveShoppingMessage(payload);
    }

    @Override
    public void WebcastLiveEcomGeneralMessage(byte[] payload) throws InvalidProtocolBufferException {
        try {
            LiveEcomGeneralMessage msg = LiveEcomGeneralMessage.parseFrom(payload);
            if (onEcomData != null) {
                LiveEcomDataVO vo = new LiveEcomDataVO();
                vo.setEcomType("ECOM");
                vo.setRoomId(msg.getRoomId());
                vo.setMsgType(msg.getMsgType());
                vo.setTimestamp(msg.getTimestamp());
                vo.setRawJson(safeBytesToString(msg.getContent()));
                onEcomData.accept(vo);
            }
        } catch (Exception e) {
            log.warn("解析电商通用消息失败: taskId={}", taskId, e);
        }
        super.WebcastLiveEcomGeneralMessage(payload);
    }

    @Override
    public void WebcastLiveEcomRankListMessage(byte[] payload) throws InvalidProtocolBufferException {
        try {
            LiveEcomRankListMessage msg = LiveEcomRankListMessage.parseFrom(payload);
            LiveEcomDataVO vo = new LiveEcomDataVO();
            vo.setEcomType("RANK");
            vo.setRoomId(msg.getRoomId());
            vo.setTimestamp(msg.getTimestamp());
            vo.setRawJson(safeBytesToString(msg.getContent()));
            // 榜单数据走独立回调，不存 DB
            if (onRankData != null) {
                onRankData.accept(vo);
            }
            // 同时走 ecom 回调（仅 WebSocket 推送）
            if (onEcomData != null) {
                onEcomData.accept(vo);
            }
        } catch (Exception e) {
            log.warn("解析电商排行榜消息失败: taskId={}", taskId, e);
        }
        super.WebcastLiveEcomRankListMessage(payload);
    }

    @Override
    public void WebcastRoomDataSyncMessage(byte[] payload) throws InvalidProtocolBufferException {
        try {
            RoomDataSyncMessage msg = RoomDataSyncMessage.parseFrom(payload);
            if (onEcomData != null) {
                String content = safeBytesToString(msg.getContent());
                // 仅推送有实质内容的消息
                if (content != null && !content.isEmpty()) {
                    LiveEcomDataVO vo = new LiveEcomDataVO();
                    vo.setEcomType("SYNC");
                    vo.setRoomId(msg.getRoomId());
                    vo.setTimestamp(msg.getTimestamp());
                    vo.setRawJson(content);
                    onEcomData.accept(vo);
                }
            }
        } catch (Exception e) {
            log.warn("解析数据同步消息失败: taskId={}", taskId, e);
        }
        super.WebcastRoomDataSyncMessage(payload);
    }

    // ==================== 工具方法 ====================

    private String extractAvatar(User user) {
        try {
            Image avatar = user.getAvatarThumb();
            if (avatar != null && avatar.getUrlListCount() > 0) {
                return avatar.getUrlList(0);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String safeBytesToString(com.google.protobuf.ByteString bytes) {
        if (bytes == null || bytes.isEmpty()) {
            return null;
        }
        try {
            return bytes.toStringUtf8();
        } catch (Exception e) {
            return bytes.toString();
        }
    }
}
