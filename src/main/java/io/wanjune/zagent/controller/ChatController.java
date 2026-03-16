package io.wanjune.zagent.controller;

import io.wanjune.zagent.common.Result;
import io.wanjune.zagent.model.dto.ChatRequest;
import io.wanjune.zagent.model.vo.ChatMessageVO;
import io.wanjune.zagent.service.ChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI对话控制器, 提供同步对话和SSE流式对话接口
 *
 * @author zagent
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    /**
     * 同步对话接口, POST /api/chat
     *
     * @param request 对话请求参数
     * @return 包含AI回复消息的统一响应
     */
    @PostMapping
    public Result<ChatMessageVO> chat(@RequestBody ChatRequest request) {
        try {
            ChatMessageVO result = chatService.chat(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Chat error", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * SSE流式对话接口, GET /api/chat/stream, 返回实时token流
     *
     * @param clientId       AI客户端ID
     * @param message        用户消息内容
     * @param conversationId 对话ID（可选）, 用于保持对话记忆连续性
     * @return SSE事件发射器, 实时推送AI生成的token
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String clientId,
                                  @RequestParam String message,
                                  @RequestParam(required = false) String conversationId) {
        ChatRequest request = new ChatRequest();
        request.setClientId(clientId);
        request.setMessage(message);
        request.setConversationId(conversationId);
        return chatService.chatStream(request);
    }

}
