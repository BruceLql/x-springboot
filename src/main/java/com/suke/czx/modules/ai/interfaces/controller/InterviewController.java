package com.suke.czx.modules.ai.interfaces.controller;

import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.ai.application.service.InterviewAppService;
import com.suke.czx.modules.ai.domain.entity.AiChatSession;
import com.suke.czx.modules.ai.infrastructure.convert.AiConvert;
import com.suke.czx.modules.ai.interfaces.dto.command.InterviewAnswerCommand;
import com.suke.czx.modules.ai.interfaces.dto.command.InterviewSessionCommand;
import com.suke.czx.modules.ai.interfaces.dto.command.InterviewStartCommand;
import com.suke.czx.modules.ai.interfaces.vo.ChatSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模拟面试 Controller
 */
@RestController
@RequestMapping("/ai/interview")
@RequiredArgsConstructor
@Tag(name = "AI模拟面试", description = "进阶模拟面试：出题、评分、追问、报告")
public class InterviewController extends AbstractController {

    private final InterviewAppService interviewAppService;
    private final AiConvert aiConvert;

    @PostMapping("/start")
    @Operation(summary = "开始面试")
    public R start(@RequestBody @Valid InterviewStartCommand command) {
        Map<String, Object> result = interviewAppService.startInterview(
                getUserId(), command.getDirection()
        );
        return R.ok().setData(result);
    }

    @PostMapping("/answer")
    @Operation(summary = "提交回答")
    public R answer(@RequestBody @Valid InterviewAnswerCommand command) {
        try {
            Map<String, Object> result = interviewAppService.submitAnswer(
                    command.getSessionId(), command.getAnswer()
            );
            return R.ok().setData(result);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/skip")
    @Operation(summary = "跳过当前题")
    public R skip(@RequestBody @Valid InterviewSessionCommand command) {
        try {
            Map<String, Object> result = interviewAppService.skipQuestion(command.getSessionId());
            return R.ok().setData(result);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/end")
    @Operation(summary = "手动结束面试并生成报告")
    public R end(@RequestBody @Valid InterviewSessionCommand command) {
        try {
            Map<String, Object> result = interviewAppService.endInterview(command.getSessionId());
            return R.ok().setData(result);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @GetMapping("/sessions")
    @Operation(summary = "获取我的面试记录")
    public R listSessions() {
        List<AiChatSession> sessions = interviewAppService.listInterviewSessions(getUserId());
        List<ChatSessionVO> vos = aiConvert.toChatSessionVOList(sessions);
        return R.ok().setData(vos);
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "获取面试详情")
    public R getDetail(@PathVariable String id) {
        try {
            Map<String, Object> detail = interviewAppService.getInterviewDetail(id);
            return R.ok().setData(detail);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }
}
