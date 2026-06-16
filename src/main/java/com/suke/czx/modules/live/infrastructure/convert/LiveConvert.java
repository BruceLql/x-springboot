package com.suke.czx.modules.live.infrastructure.convert;

import com.suke.czx.modules.live.domain.command.CreateLiveTaskCommand;
import com.suke.czx.modules.live.domain.command.UpdateLiveTaskCommand;
import com.suke.czx.modules.live.domain.entity.*;
import com.suke.czx.modules.live.domain.enums.PlatformEnum;
import com.suke.czx.modules.live.domain.enums.TaskStatusEnum;
import com.suke.czx.modules.live.interfaces.vo.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 直播对象转换器
 */
@Mapper(componentModel = "spring")
public interface LiveConvert {

    LiveConvert INSTANCE = Mappers.getMapper(LiveConvert.class);

    // ==================== LiveTask 转换 ====================

    /**
     * Entity -> VO
     */
    @Mapping(target = "platformDesc", expression = "java(getPlatformDesc(task.getPlatform()))")
    @Mapping(target = "taskStatus", expression = "java(task.getTaskStatus() != null ? task.getTaskStatus().getCode() : null)")
    @Mapping(target = "taskStatusDesc", expression = "java(getTaskStatusDesc(task.getTaskStatus()))")
    LiveTaskVO toVO(LiveTask task);

    /**
     * Entity列表 -> VO列表
     */
    List<LiveTaskVO> toVOList(List<LiveTask> taskList);

    /**
     * Entity -> DetailVO
     */
    @Mapping(target = "platformDesc", expression = "java(getPlatformDesc(task.getPlatform()))")
    @Mapping(target = "taskStatus", expression = "java(task.getTaskStatus() != null ? task.getTaskStatus().getCode() : null)")
    @Mapping(target = "taskStatusDesc", expression = "java(getTaskStatusDesc(task.getTaskStatus()))")
    @Mapping(target = "statistics", ignore = true)
    LiveTaskDetailVO toDetailVO(LiveTask task);

    /**
     * CreateCommand -> Entity
     */
    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "roomId", ignore = true)
    @Mapping(target = "roomName", ignore = true)
    @Mapping(target = "anchorName", ignore = true)
    @Mapping(target = "taskStatus", constant = "PENDING")
    @Mapping(target = "startTime", ignore = true)
    @Mapping(target = "stopTime", ignore = true)
    @Mapping(target = "expireTime", ignore = true)
    @Mapping(target = "errorMsg", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    LiveTask toEntity(CreateLiveTaskCommand command);

    /**
     * UpdateCommand -> 更新Entity
     */
    @Mapping(target = "platform", ignore = true)
    @Mapping(target = "roomId", ignore = true)
    @Mapping(target = "roomName", ignore = true)
    @Mapping(target = "anchorName", ignore = true)
    @Mapping(target = "taskStatus", ignore = true)
    @Mapping(target = "startTime", ignore = true)
    @Mapping(target = "stopTime", ignore = true)
    @Mapping(target = "expireTime", ignore = true)
    @Mapping(target = "errorMsg", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void updateEntity(@MappingTarget LiveTask task, UpdateLiveTaskCommand command);

    // ==================== LiveChat 转换 ====================

    /**
     * Entity -> VO
     */
    LiveChatVO toChatVO(LiveChat chat);

    /**
     * Entity列表 -> VO列表
     */
    List<LiveChatVO> toChatVOList(List<LiveChat> chatList);

    // ==================== LiveGift 转换 ====================

    /**
     * Entity -> VO
     */
    LiveGiftVO toGiftVO(LiveGift gift);

    /**
     * Entity列表 -> VO列表
     */
    List<LiveGiftVO> toGiftVOList(List<LiveGift> giftList);

    // ==================== LiveLike 转换 ====================

    /**
     * Entity -> VO
     */
    LiveLikeVO toLikeVO(LiveLike like);

    /**
     * Entity列表 -> VO列表
     */
    List<LiveLikeVO> toLikeVOList(List<LiveLike> likeList);

    // ==================== LiveFollow 转换 ====================

    /**
     * Entity -> VO
     */
    LiveFollowVO toFollowVO(LiveFollow follow);

    /**
     * Entity列表 -> VO列表
     */
    List<LiveFollowVO> toFollowVOList(List<LiveFollow> followList);

    // ==================== LiveUserEnter 转换 ====================

    /**
     * Entity -> VO
     */
    LiveUserEnterVO toUserEnterVO(LiveUserEnter enter);

    /**
     * Entity列表 -> VO列表
     */
    List<LiveUserEnterVO> toUserEnterVOList(List<LiveUserEnter> enterList);

    // ==================== 统计数据转换 ====================

    /**
     * 统计数据 -> StatisticsVO
     */
    @Mapping(target = "chatUserCount", ignore = true)
    @Mapping(target = "giftUserCount", ignore = true)
    @Mapping(target = "totalGiftCount", ignore = true)
    @Mapping(target = "totalLikeCount", ignore = true)
    LiveTaskStatisticsVO toStatisticsVO(Long chatCount, Long giftCount, Long likeCount, Long followCount, Long userEnterCount);

    // ==================== 枚举描述方法 ====================

    /**
     * 获取平台描述
     */
    default String getPlatformDesc(PlatformEnum platform) {
        if (platform == null) {
            return "";
        }
        return platform.getDesc();
    }

    /**
     * 获取任务状态描述
     */
    default String getTaskStatusDesc(TaskStatusEnum taskStatus) {
        if (taskStatus == null) {
            return "";
        }
        return taskStatus.getDesc();
    }
}
