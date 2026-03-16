package io.wanjune.zagent.agent;

import io.wanjune.zagent.model.entity.AiAgentFlowConfig;
import io.wanjune.zagent.model.vo.AgentResultVO;
import io.wanjune.zagent.service.AiClientAssemblyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent顺序流程执行器, 按flow配置的sequence顺序依次调用ChatClient,
 * 每一步的输出作为下一步的输入
 *
 * @author zagent
 */
@Slf4j
@Component
public class AgentExecutor {

    @Resource
    private AiClientAssemblyService aiClientAssemblyService;

    /**
     * 执行Agent流程, 返回每个步骤的执行结果列表
     *
     * @param flowConfigs    按顺序排列的流程步骤配置
     * @param initialInput   用户的初始输入
     * @param conversationId 对话ID, 用于对话记忆
     * @return 每个步骤的执行结果列表
     */
    public List<AgentResultVO.StepResult> execute(List<AiAgentFlowConfig> flowConfigs,
                                                    String initialInput,
                                                    String conversationId) {
        List<AgentResultVO.StepResult> results = new ArrayList<>();
        String currentInput = initialInput;

        for (AiAgentFlowConfig step : flowConfigs) {
            String clientId = String.valueOf(step.getClientId());
            log.info("Agent step {} - clientId: {}, input length: {}", step.getSequence(), clientId, currentInput.length());

            try {
                ChatClient chatClient = aiClientAssemblyService.getOrBuildChatClient(clientId);

                String output = chatClient.prompt(currentInput)
                        .system(s -> s.param("current_date", LocalDate.now().toString()))
                        .advisors(a -> a
                                .param("chat_memory_conversation_id", conversationId)
                                .param("chat_memory_response_size", 100))
                        .call()
                        .content();

                results.add(AgentResultVO.StepResult.builder()
                        .sequence(step.getSequence())
                        .clientId(clientId)
                        .input(currentInput)
                        .output(output)
                        .build());

                // Pass output to next step
                currentInput = output;

                log.info("Agent step {} completed, output length: {}", step.getSequence(), output != null ? output.length() : 0);
            } catch (Exception e) {
                log.error("Agent step {} failed for clientId: {}", step.getSequence(), clientId, e);
                results.add(AgentResultVO.StepResult.builder()
                        .sequence(step.getSequence())
                        .clientId(clientId)
                        .input(currentInput)
                        .output("ERROR: " + e.getMessage())
                        .build());
                break;
            }
        }

        return results;
    }

}
