package com.suke.czx.modules.live.interfaces.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.suke.czx.common.annotation.ResourceAuth;
import com.suke.czx.common.annotation.SysLog;
import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.live.application.service.LiveTaskApplicationService;
import com.suke.czx.modules.live.domain.command.CreateLiveTaskCommand;
import com.suke.czx.modules.live.domain.command.UpdateLiveTaskCommand;
import com.suke.czx.modules.live.infrastructure.buffer.LiveEventBuffer;
import com.suke.czx.modules.live.interfaces.dto.query.LiveDataPageQuery;
import com.suke.czx.modules.live.interfaces.dto.query.LiveTaskPageQuery;
import com.suke.czx.modules.live.interfaces.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 直播任务Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/live/task")
@Tag(name = "LiveTaskController", description = "直播任务管理")
public class LiveTaskController extends AbstractController {

    private final LiveTaskApplicationService liveTaskApplicationService;
    private final LiveEventBuffer eventBuffer;

    /**
     * 分页查询任务列表
     */
    @GetMapping("/list")
    @Operation(summary = "任务列表", description = "分页查询直播任务列表")
    @ResourceAuth(value = "直播任务列表", module = "live")
    public R list(@Validated LiveTaskPageQuery query) {
        IPage<LiveTaskVO> page = liveTaskApplicationService.queryPage(query);
        return R.ok().setData(page);
    }

    /**
     * 查询任务详情（含统计）
     */
    @GetMapping("/info/{taskId}")
    @Operation(summary = "任务详情", description = "根据ID查询任务详情(含统计数据)")
    @ResourceAuth(value = "直播任务详情", module = "live")
    public R info(@PathVariable String taskId) {
        LiveTaskDetailVO vo = liveTaskApplicationService.getDetail(taskId);
        return R.ok().setData(vo);
    }

    /**
     * 创建任务
     */
    @SysLog("创建直播任务")
    @PostMapping("/create")
    @Operation(summary = "创建任务", description = "创建新的直播监控任务")
    @ResourceAuth(value = "创建直播任务", module = "live")
    public R create(@Validated @RequestBody CreateLiveTaskCommand command) {
        LiveTaskVO vo = liveTaskApplicationService.create(command);
        return R.ok().setData(vo);
    }

    /**
     * 更新任务
     */
    @SysLog("更新直播任务")
    @PutMapping("/update")
    @Operation(summary = "更新任务", description = "更新直播监控任务")
    @ResourceAuth(value = "更新直播任务", module = "live")
    public R update(@Validated @RequestBody UpdateLiveTaskCommand command) {
        liveTaskApplicationService.update(command);
        return R.ok("任务更新成功");
    }

    /**
     * 启动任务
     */
    @SysLog("启动直播任务")
    @PostMapping("/start/{taskId}")
    @Operation(summary = "启动任务", description = "启动直播监控任务")
    @ResourceAuth(value = "启动直播任务", module = "live")
    public R start(@PathVariable String taskId) {
        liveTaskApplicationService.startTask(taskId);
        return R.ok("任务启动成功");
    }

    /**
     * 停止任务
     */
    @SysLog("停止直播任务")
    @PostMapping("/stop/{taskId}")
    @Operation(summary = "停止任务", description = "停止直播监控任务")
    @ResourceAuth(value = "停止直播任务", module = "live")
    public R stop(@PathVariable String taskId) {
        liveTaskApplicationService.stopTask(taskId);
        return R.ok("任务停止成功");
    }

    /**
     * 删除任务
     */
    @SysLog("删除直播任务")
    @DeleteMapping("/delete/{taskId}")
    @Operation(summary = "删除任务", description = "删除直播监控任务")
    @ResourceAuth(value = "删除直播任务", module = "live")
    public R delete(@PathVariable String taskId) {
        liveTaskApplicationService.deleteTask(taskId);
        return R.ok("任务删除成功");
    }

    /**
     * 清空任务采集数据
     */
    @SysLog("清空直播任务数据")
    @DeleteMapping("/data/clear/{taskId}")
    @Operation(summary = "清空任务数据", description = "清空指定任务的采集数据")
    @ResourceAuth(value = "清空直播数据", module = "live")
    public R clearData(@PathVariable String taskId) {
        liveTaskApplicationService.clearTaskData(taskId);
        return R.ok("任务数据清空成功");
    }

    /**
     * 查询任务统计数据
     */
    @GetMapping("/statistics/{taskId}")
    @Operation(summary = "任务统计", description = "查询任务的数据统计")
    @ResourceAuth(value = "直播任务统计", module = "live")
    public R statistics(@PathVariable String taskId) {
        LiveTaskStatisticsVO statistics = liveTaskApplicationService.getStatistics(taskId);
        return R.ok().setData(statistics);
    }

    /**
     * 查询直播间实时数据（在线人数+排行榜）
     */
    @GetMapping("/liveData/{taskId}")
    @Operation(summary = "直播间实时数据", description = "查询直播间在线人数和排行榜")
    @ResourceAuth(value = "直播实时数据", module = "live")
    public R liveData(@PathVariable String taskId) {
        LiveRoomLiveDataVO liveData = liveTaskApplicationService.getRoomLiveData(taskId);
        return R.ok().setData(liveData);
    }

    /**
     * 查询聊天数据
     */
    @GetMapping("/data/chat")
    @Operation(summary = "聊天数据", description = "查询直播聊天数据")
    @ResourceAuth(value = "直播聊天数据", module = "live")
    public R chatList(@Validated LiveDataPageQuery query) {
        IPage<LiveChatVO> page = liveTaskApplicationService.queryChat(query);
        return R.ok().setData(page);
    }

    /**
     * 查询礼物数据
     */
    @GetMapping("/data/gift")
    @Operation(summary = "礼物数据", description = "查询直播礼物数据")
    @ResourceAuth(value = "直播礼物数据", module = "live")
    public R giftList(@Validated LiveDataPageQuery query) {
        IPage<LiveGiftVO> page = liveTaskApplicationService.queryGift(query);
        return R.ok().setData(page);
    }

    /**
     * 查询点赞数据
     */
    @GetMapping("/data/like")
    @Operation(summary = "点赞数据", description = "查询直播点赞数据")
    @ResourceAuth(value = "直播点赞数据", module = "live")
    public R likeList(@Validated LiveDataPageQuery query) {
        IPage<LiveLikeVO> page = liveTaskApplicationService.queryLike(query);
        return R.ok().setData(page);
    }

    /**
     * 查询关注数据
     */
    @GetMapping("/data/follow")
    @Operation(summary = "关注数据", description = "查询直播关注数据")
    @ResourceAuth(value = "直播关注数据", module = "live")
    public R followList(@Validated LiveDataPageQuery query) {
        IPage<LiveFollowVO> page = liveTaskApplicationService.queryFollow(query);
        return R.ok().setData(page);
    }

    /**
     * 查询用户进入数据
     */
    @GetMapping("/data/userEnter")
    @Operation(summary = "用户进入数据", description = "查询直播用户进入数据")
    @ResourceAuth(value = "直播用户进入数据", module = "live")
    public R userEnterList(@Validated LiveDataPageQuery query) {
        IPage<LiveUserEnterVO> page = liveTaskApplicationService.queryUserEnter(query);
        return R.ok().setData(page);
    }

    /**
     * 查询电商数据
     */
    @GetMapping("/data/ecom")
    @Operation(summary = "电商数据", description = "查询直播电商数据(下单/上下架/促销等)")
    @ResourceAuth(value = "直播电商数据", module = "live")
    public R ecomList(@Validated LiveDataPageQuery query) {
        IPage<LiveEcomDataVO> page = liveTaskApplicationService.queryEcom(query);
        return R.ok().setData(page);
    }

    /**
     * 查询电商榜单实时数据
     */
    @GetMapping("/rankData/{taskId}")
    @Operation(summary = "电商榜单", description = "查询直播间电商榜单实时数据")
    @ResourceAuth(value = "直播榜单数据", module = "live")
    public R rankData(@PathVariable String taskId) {
        var data = liveTaskApplicationService.getRankData(taskId);
        return R.ok().setData(data);
    }

    /**
     * 查询缓冲区状态（监控用）
     */
    @GetMapping("/buffer/status")
    @Operation(summary = "缓冲区状态", description = "查询事件缓冲区状态")
    @ResourceAuth(value = "直播缓冲区监控", module = "live")
    public R bufferStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("chatQueueSize", eventBuffer.getChatQueueSize());
        status.put("giftQueueSize", eventBuffer.getGiftQueueSize());
        status.put("likeQueueSize", eventBuffer.getLikeQueueSize());
        status.put("followQueueSize", eventBuffer.getFollowQueueSize());
        status.put("userEnterQueueSize", eventBuffer.getUserEnterQueueSize());
        status.put("ecomQueueSize", eventBuffer.getEcomQueueSize());
        status.put("totalProcessed", eventBuffer.getTotalProcessed());
        status.put("totalDropped", eventBuffer.getTotalDropped());
        status.put("hasPendingData", eventBuffer.hasPendingData());
        return R.ok().setData(status);
    }
}
