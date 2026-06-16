package com.suke.czx.modules.ai.interfaces.controller;

import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.R;
import com.suke.czx.modules.ai.application.service.ChatAppService;
import com.suke.czx.modules.ai.domain.entity.AiChatMessage;
import com.suke.czx.modules.ai.domain.entity.AiChatSession;
import com.suke.czx.modules.ai.infrastructure.convert.AiConvert;
import com.suke.czx.modules.ai.interfaces.dto.command.ChatCommand;
import com.suke.czx.modules.ai.interfaces.dto.command.CreateSessionCommand;
import com.suke.czx.modules.ai.interfaces.vo.ChatMessageVO;
import com.suke.czx.modules.ai.interfaces.vo.ChatSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * AI 对话 Controller
 */
@Slf4j
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
@Tag(name = "AI对话", description = "RAG 智能问答、流式对话")
public class ChatController extends AbstractController {

    private final ChatAppService chatAppService;
    private final AiConvert aiConvert;

    // ==================== 会话管理 ====================

    @GetMapping("/sessions")
    @Operation(summary = "获取我的会话列表")
    public R listSessions() {
        List<AiChatSession> sessions = chatAppService.listSessions(getUserId());
        List<ChatSessionVO> vos = aiConvert.toChatSessionVOList(sessions);
        return R.ok().setData(vos);
    }

    @PostMapping("/sessions")
    @Operation(summary = "创建新会话")
    public R createSession(@RequestBody @Valid CreateSessionCommand command) {
        AiChatSession session = chatAppService.createSession(getUserId(), command.getTitle());
        return R.ok().setData(aiConvert.toVO(session));
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "删除会话")
    public R deleteSession(@PathVariable String id) {
        chatAppService.deleteSession(id);
        return R.ok("删除成功");
    }

    @GetMapping("/sessions/{id}/messages")
    @Operation(summary = "获取会话历史消息")
    public R getMessages(@PathVariable String id) {
        List<AiChatMessage> messages = chatAppService.getMessages(id);
        List<ChatMessageVO> vos = aiConvert.toChatMessageVOList(messages);
        return R.ok().setData(vos);
    }

    // ==================== RAG 问答 ====================

    @PostMapping("/send")
    @Operation(summary = "发送消息（非流式）")
    public R send(@RequestBody @Valid ChatCommand command) {
        Map<String, Object> result = chatAppService.send(
                command.getSessionId(), getUserId(), command.getContent()
        );
        return R.ok().setData(result);
    }

    @PostMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息（SSE流式）")
    public Flux<ServerSentEvent<String>> sendStream(@RequestBody @Valid ChatCommand command) {
        String userId = getUserId();
        if (userId == null || userId.isBlank()) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("用户未认证，请先登录")
                    .build());
        }
        return chatAppService.sendStream(command.getSessionId(), userId, command.getContent())
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build())
                .onErrorResume(error -> {
                    log.error("Stream error: {}", error.getMessage());
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("流式响应异常: " + error.getMessage())
                            .build());
                });
    }
}
