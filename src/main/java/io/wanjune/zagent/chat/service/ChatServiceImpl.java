package io.wanjune.zagent.chat.service;

import io.wanjune.zagent.model.dto.ChatRequest;
import io.wanjune.zagent.model.vo.ChatMessageVO;
import io.wanjune.zagent.chat.assembly.AiClientAssemblyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AI对话服务实现, 通过AiClientAssemblyService获取ChatClient进行对话。
 * <p>支持同步调用和SSE流式推送两种模式, 自动管理对话ID和上下文参数。</p>
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private AiClientAssemblyService aiClientAssemblyService;
    @Resource(name = "executorService")
    private ThreadPoolExecutor executorService;

    /** {@inheritDoc} */
    @Override
    public ChatMessageVO chat(ChatRequest request) {
        ChatClient chatClient = aiClientAssemblyService.getOrBuildChatClient(request.getClientId());

        String conversationId = resolveConversationId(request);

        String content = chatClient.prompt(request.getMessage())
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .advisors(a -> a
                        .param("chat_memory_conversation_id", conversationId)
                        .param("chat_memory_response_size", 100))
                .call()
                .content();

        return ChatMessageVO.builder()
                .role("assistant")
                .content(content)
                .conversationId(conversationId)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public SseEmitter chatStream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout

        executorService.execute(() -> {
            try {
                ChatClient chatClient = aiClientAssemblyService.getOrBuildChatClient(request.getClientId());
                String conversationId = resolveConversationId(request);

                Flux<ChatClientResponse> flux = chatClient.prompt(request.getMessage())
                        .system(s -> s.param("current_date", LocalDate.now().toString()))
                        .advisors(a -> a
                                .param("chat_memory_conversation_id", conversationId)
                                .param("chat_memory_response_size", 100))
                        .stream()
                        .chatClientResponse();

                flux.subscribe(
                        response -> {
                            try {
                                if (response.chatResponse() != null
                                        && response.chatResponse().getResult() != null
                                        && response.chatResponse().getResult().getOutput() != null) {
                                    String text = response.chatResponse().getResult().getOutput().getText();
                                    if (text != null) {
                                        emitter.send(SseEmitter.event().data(text));
                                    }
                                }
                            } catch (IOException e) {
                                log.error("SSE send error", e);
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.error("Stream error", error);
                            emitter.completeWithError(error);
                        },
                        emitter::complete
                );
            } catch (Exception e) {
                log.error("Chat stream error", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 解析对话ID, 未提供则自动生成UUID。
     * <p>如果请求中包含conversationId则直接使用, 否则生成一个新的UUID作为对话标识,
     * 用于关联同一对话的多轮消息。</p>
     *
     * @param request 对话请求
     * @return 对话ID
     */
    private String resolveConversationId(ChatRequest request) {
        return (request.getConversationId() != null && !request.getConversationId().isBlank())
                ? request.getConversationId()
                : UUID.randomUUID().toString();
    }

}
