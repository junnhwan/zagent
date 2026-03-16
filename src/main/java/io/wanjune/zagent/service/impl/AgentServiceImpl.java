package io.wanjune.zagent.service.impl;

import io.wanjune.zagent.agent.AgentExecutor;
import io.wanjune.zagent.mapper.AiAgentFlowConfigMapper;
import io.wanjune.zagent.mapper.AiAgentMapper;
import io.wanjune.zagent.model.dto.AgentRunRequest;
import io.wanjune.zagent.model.entity.AiAgent;
import io.wanjune.zagent.model.entity.AiAgentFlowConfig;
import io.wanjune.zagent.model.vo.AgentResultVO;
import io.wanjune.zagent.service.AiClientAssemblyService;
import io.wanjune.zagent.service.AgentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Agent编排服务实现, 根据ai_agent_flow_config配置的执行顺序依次调用ChatClient。
 * <p>同步模式下通过AgentExecutor按步骤顺序执行; 流式模式下取流程最后一步的ChatClient进行SSE推送。</p>
 */
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    @Resource
    private AiAgentMapper aiAgentMapper;
    @Resource
    private AiAgentFlowConfigMapper aiAgentFlowConfigMapper;
    @Resource
    private AgentExecutor agentExecutor;
    @Resource
    private AiClientAssemblyService aiClientAssemblyService;
    @Resource(name = "executorService")
    private ThreadPoolExecutor executorService;

    /** {@inheritDoc} */
    @Override
    public AgentResultVO run(AgentRunRequest request) {
        AiAgent agent = aiAgentMapper.selectByAgentId(request.getAgentId());
        if (agent == null) {
            throw new RuntimeException("Agent not found: " + request.getAgentId());
        }

        List<AiAgentFlowConfig> flowConfigs = aiAgentFlowConfigMapper.selectByAgentId(Long.parseLong(agent.getAgentId()));
        if (flowConfigs == null || flowConfigs.isEmpty()) {
            throw new RuntimeException("No flow config for agent: " + request.getAgentId());
        }

        String conversationId = request.getConversationId() != null
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        List<AgentResultVO.StepResult> steps = agentExecutor.execute(flowConfigs, request.getInput(), conversationId);

        String finalOutput = steps.isEmpty() ? "" : steps.get(steps.size() - 1).getOutput();

        return AgentResultVO.builder()
                .agentId(agent.getAgentId())
                .agentName(agent.getAgentName())
                .finalOutput(finalOutput)
                .steps(steps)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public SseEmitter runStream(AgentRunRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        executorService.execute(() -> {
            try {
                AiAgent agent = aiAgentMapper.selectByAgentId(request.getAgentId());
                if (agent == null) {
                    emitter.send(SseEmitter.event().data("ERROR: Agent not found"));
                    emitter.complete();
                    return;
                }

                List<AiAgentFlowConfig> flowConfigs = aiAgentFlowConfigMapper.selectByAgentId(Long.parseLong(agent.getAgentId()));
                if (flowConfigs == null || flowConfigs.isEmpty()) {
                    emitter.send(SseEmitter.event().data("ERROR: No flow config"));
                    emitter.complete();
                    return;
                }

                String conversationId = request.getConversationId() != null
                        ? request.getConversationId()
                        : UUID.randomUUID().toString();

                // For streaming, only use the last client in the flow (chat_stream mode)
                AiAgentFlowConfig lastStep = flowConfigs.get(flowConfigs.size() - 1);
                String clientId = String.valueOf(lastStep.getClientId());
                ChatClient chatClient = aiClientAssemblyService.getOrBuildChatClient(clientId);

                Flux<ChatClientResponse> flux = chatClient.prompt(request.getInput())
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
                                emitter.completeWithError(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );
            } catch (Exception e) {
                log.error("Agent stream error", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

}
