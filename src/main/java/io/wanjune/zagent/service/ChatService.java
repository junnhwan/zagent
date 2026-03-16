package io.wanjune.zagent.service;

import io.wanjune.zagent.model.dto.ChatRequest;
import io.wanjune.zagent.model.vo.ChatMessageVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI对话服务接口, 支持同步和SSE流式两种对话模式。
 */
public interface ChatService {

    /**
     * 同步对话 - 等待AI完整响应后返回。
     * <p>发送用户消息给AI, 阻塞等待完整响应内容后一次性返回结果。</p>
     *
     * @param request 对话请求, 包含clientId、消息内容和可选的conversationId
     * @return 包含AI回复内容和对话ID的 {@link ChatMessageVO}
     */
    ChatMessageVO chat(ChatRequest request);

    /**
     * 流式对话 - 通过SseEmitter实时推送AI生成的token。
     * <p>发送用户消息给AI, 通过SSE（Server-Sent Events）实时推送AI生成的每个token,
     * 实现打字机效果的流式输出。</p>
     *
     * @param request 对话请求, 包含clientId、消息内容和可选的conversationId
     * @return SSE发射器 {@link SseEmitter}, 客户端可通过EventSource接收流式数据
     */
    SseEmitter chatStream(ChatRequest request);

}
