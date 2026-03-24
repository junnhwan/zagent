package io.wanjune.zagent.agent.strategy.support;

import com.alibaba.fastjson.JSON;
import io.wanjune.zagent.agent.strategy.IExecuteStrategy.StageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 策略共用工具方法，消除各 Strategy 实现中的重复代码。
 */
@Slf4j
public final class StrategyHelper {

    private StrategyHelper() {}

    public static String abbreviateForLog(String text) {
        if (text == null) {
            return "<null>";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() > 200 ? normalized.substring(0, 200) + "...(截断)" : normalized;
    }

    public static void sendStageEvent(SseEmitter emitter, String stage, String status,
                                       int step, int totalSteps, String content, String sessionId) {
        sendStageEvent(emitter, stage, status, step, totalSteps, content, null, sessionId);
    }

    public static void sendStageEvent(SseEmitter emitter, String stage, String status,
                                       int step, int totalSteps, String content, Object payload, String sessionId) {
        if (emitter == null) return;
        try {
            StageEvent event = StageEvent.builder()
                    .stage(stage).status(status).step(step).totalSteps(totalSteps)
                    .content(content).payload(payload).sessionId(sessionId).build();
            emitter.send(SseEmitter.event().data(JSON.toJSONString(event)));
        } catch (Exception e) {
            log.error("发送SSE事件失败", e);
        }
    }

    public static String getStepPrompt(Map<String, String> stepPromptMap, String clientType, String defaultPrompt) {
        if (stepPromptMap == null) return defaultPrompt;
        String dbPrompt = stepPromptMap.get(clientType);
        return (dbPrompt != null && !dbPrompt.isBlank()) ? dbPrompt : defaultPrompt;
    }

    public static String callClient(ChatClient client, String prompt, String conversationId) {
        return client.prompt(prompt)
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .advisors(a -> a
                        .param("chat_memory_conversation_id", conversationId)
                        .param("chat_memory_response_size", 1024))
                .call()
                .content();
    }

    /**
     * 流式调用ChatClient, 每个token实时推送SSE事件。
     * <p>当emitter为null时退化为同步调用callClient。</p>
     */
    public static String callClientStream(ChatClient client, String prompt, String conversationId,
                                           SseEmitter emitter, String stage, int step, int totalSteps) {
        if (emitter == null) {
            return callClient(client, prompt, conversationId);
        }

        StringBuilder result = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Flux<ChatClientResponse> flux = client.prompt(prompt)
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .advisors(a -> a
                        .param("chat_memory_conversation_id", conversationId)
                        .param("chat_memory_response_size", 1024))
                .stream()
                .chatClientResponse();

        flux.subscribe(
                response -> {
                    try {
                        String text = response.chatResponse() != null
                                && response.chatResponse().getResult() != null
                                && response.chatResponse().getResult().getOutput() != null
                                ? response.chatResponse().getResult().getOutput().getText()
                                : null;
                        if (text != null && !text.isEmpty()) {
                            result.append(text);
                            StageEvent tokenEvent = StageEvent.builder()
                                    .type("token").stage(stage).status("active")
                                    .step(step).totalSteps(totalSteps)
                                    .content(text).sessionId(conversationId).build();
                            emitter.send(SseEmitter.event().data(JSON.toJSONString(tokenEvent)));
                        }
                    } catch (Exception e) {
                        log.error("发送token事件失败", e);
                    }
                },
                error -> {
                    log.error("流式调用异常, stage={}", stage, error);
                    errorRef.set(error);
                    latch.countDown();
                },
                latch::countDown
        );

        try {
            if (!latch.await(5, TimeUnit.MINUTES)) {
                log.warn("流式调用超时, stage={}", stage);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("流式调用等待被中断, stage={}", stage, e);
        }

        if (errorRef.get() != null && result.isEmpty()) {
            log.warn("流式调用失败, 降级为同步调用, stage={}", stage);
            return callClient(client, prompt, conversationId);
        }

        return result.toString();
    }
}
