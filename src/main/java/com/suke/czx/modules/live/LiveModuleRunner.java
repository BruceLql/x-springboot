package com.suke.czx.modules.live;

import com.suke.czx.modules.live.domain.command.CreateLiveTaskCommand;
import com.suke.czx.modules.live.domain.enums.PlatformEnum;
import com.suke.czx.modules.live.application.service.LiveTaskApplicationService;
import com.suke.czx.modules.live.interfaces.vo.LiveTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 直播模块启动示例
 * 演示如何使用直播任务服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveModuleRunner implements CommandLineRunner {

    private final LiveTaskApplicationService liveTaskApplicationService;

    @Override
    public void run(String... args) {
        log.info("=== 直播监控模块已启动 ===");
        log.info("支持功能:");
        log.info("1. 创建直播监控任务");
        log.info("2. 启动/停止监控任务");
        log.info("3. 自动超时控制(默认4小时)");
        log.info("4. 实时数据采集(聊天、礼物、点赞、关注、用户进入)");
        log.info("5. 数据查询与统计");
        log.info("=========================");
        
        // 示例: 创建一个监控任务
        // CreateLiveTaskCommand command = new CreateLiveTaskCommand();
        // command.setPlatform(PlatformEnum.DOUYIN);
        // command.setRoomUrl("https://live.douyin.com/123456789");
        // command.setCollectChat(true);
        // command.setCollectGift(true);
        // command.setCollectLike(false);
        // command.setCollectFollow(false);
        // command.setCollectUser(false);
        // command.setMaxDuration(240);
        //
        // LiveTaskVO task = liveTaskApplicationService.create(command);
        // log.info("创建任务成功: {}", task.getTaskId());
        //
        // // 启动任务
        // liveTaskApplicationService.startTask(task.getTaskId());
        // log.info("启动任务成功: {}", task.getTaskId());
    }
}
