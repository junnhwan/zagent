package io.wanjune.zagent.agent.strategy;

import io.wanjune.zagent.model.enums.ClientTypeEnum;
import io.wanjune.zagent.service.AiClientAssemblyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.Map;

/**
 * Fixed策略 - 顺序执行。
 * <p>按FlowConfig的sequence顺序依次调用ChatClient, 每一步的输出作为下一步的输入。
 * 适用于简单的链式对话场景。</p>
 *
 * @author zagent
 */
@Slf4j
@Service("fixedExecuteStrategy")
public class FixedExecuteStrategy implements IExecuteStrategy {

    @Resource
    private AiClientAssemblyService aiClientAssemblyService;

    @Override
    public String execute(ExecuteContext context, SseEmitter emitter) throws Exception {
        Map<String, String> clientTypeMap = context.getClientTypeMap();
        String currentInput = context.getUserInput();
        String lastOutput = currentInput;
        int totalSteps = clientTypeMap.size();

        // 按clientType遍历（fixed策略所有type都是default, 直接顺序执行）
        int step = 0;
        for (Map.Entry<String, String> entry : clientTypeMap.entrySet()) {
            step++;
            String clientId = entry.getValue();
            log.info("Fixed策略 - 步骤{}: clientId={}", step, clientId);

            sendStageEvent(emitter, "execution", "active", step, totalSteps, null, context.getConversationId());
            ChatClient chatClient = aiClientAssemblyService.getOrBuildChatClient(clientId);
            lastOutput = chatClient.prompt(currentInput)
                    .system(s -> s.param("current_date", LocalDate.now().toString()))
                    .advisors(a -> a
                            .param("chat_memory_conversation_id", context.getConversationId())
                            .param("chat_memory_response_size", 100))
                    .call()
                    .content();

            // 推送SSE事件
            sendStageEvent(emitter, "execution", "done", step, totalSteps, lastOutput, context.getConversationId());

            // 本步输出作为下一步输入
            currentInput = lastOutput;
            log.info("Fixed策略 - 步骤{}完成, 输出长度: {}", step, lastOutput != null ? lastOutput.length() : 0);
        }

        sendStageEvent(emitter, "complete", "done", 0, totalSteps, "执行完成", context.getConversationId());
        return lastOutput;
    }

    private void sendStageEvent(SseEmitter emitter, String stage, String status, int step, int totalSteps, String content, String sessionId) {
        if (emitter == null) return;
        try {
            StageEvent event = StageEvent.builder()
                    .stage(stage).status(status).step(step).totalSteps(totalSteps)
                    .content(content).sessionId(sessionId).build();
            emitter.send(SseEmitter.event().data(com.alibaba.fastjson.JSON.toJSONString(event)));
        } catch (Exception e) {
            log.error("发送SSE事件失败", e);
        }
    }

}
