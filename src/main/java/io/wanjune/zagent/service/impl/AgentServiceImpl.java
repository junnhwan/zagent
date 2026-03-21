package io.wanjune.zagent.service.impl;

import io.wanjune.zagent.agent.strategy.IExecuteStrategy;
import io.wanjune.zagent.mapper.AiAgentFlowConfigMapper;
import io.wanjune.zagent.mapper.AiAgentMapper;
import io.wanjune.zagent.model.dto.AgentRunRequest;
import io.wanjune.zagent.model.entity.AiAgent;
import io.wanjune.zagent.model.entity.AiAgentFlowConfig;
import io.wanjune.zagent.model.vo.AgentResultVO;
import io.wanjune.zagent.service.AgentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Agent编排服务实现, 基于策略模式分发到不同的执行策略。
 * <p>根据ai_agent表的strategy字段, 自动选择Fixed/Auto/Flow策略执行。
 * 使用Spring Map注入实现策略分发, key为bean名称。</p>
 *
 * @author zagent
 */
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    @Resource
    private AiAgentMapper aiAgentMapper;
    @Resource
    private AiAgentFlowConfigMapper aiAgentFlowConfigMapper;
    @Resource
    private Map<String, IExecuteStrategy> executeStrategyMap;
    @Resource(name = "executorService")
    private ThreadPoolExecutor executorService;

    /** 策略名称到Bean名称的映射 */
    private static final Map<String, String> STRATEGY_BEAN_MAP = Map.of(
            "fixed", "fixedExecuteStrategy",
            "auto", "autoExecuteStrategy",
            "flow", "flowExecuteStrategy"
    );

    @Override
    public AgentResultVO run(AgentRunRequest request) {
        AiAgent agent = loadAgent(request.getAgentId());
        IExecuteStrategy.ExecuteContext context = buildContext(agent, request);
        IExecuteStrategy strategy = resolveStrategy(agent.getStrategy());

        try {
            String result = strategy.execute(context, null);
            return AgentResultVO.builder()
                    .agentId(agent.getAgentId())
                    .agentName(agent.getAgentName())
                    .finalOutput(result)
                    .build();
        } catch (Exception e) {
            log.error("Agent执行失败: agentId={}", request.getAgentId(), e);
            throw new RuntimeException("Agent执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SseEmitter runStream(AgentRunRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        executorService.execute(() -> {
            try {
                AiAgent agent = loadAgent(request.getAgentId());
                IExecuteStrategy.ExecuteContext context = buildContext(agent, request);
                IExecuteStrategy strategy = resolveStrategy(agent.getStrategy());

                strategy.execute(context, emitter);
            } catch (Exception e) {
                log.error("Agent流式执行失败", e);
                try {
                    emitter.send(SseEmitter.event().data("{\"stage\":\"error\",\"content\":\"" + e.getMessage() + "\"}"));
                } catch (Exception ex) {
                    log.error("发送错误事件失败", ex);
                }
            } finally {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.error("SSE complete失败", e);
                }
            }
        });

        return emitter;
    }

    /**
     * 加载并校验Agent配置
     */
    private AiAgent loadAgent(String agentId) {
        AiAgent agent = aiAgentMapper.selectByAgentId(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Agent不存在: " + agentId);
        }
        return agent;
    }

    /**
     * 构建执行上下文: 加载FlowConfig并转换为clientType -> clientId的映射
     */
    private IExecuteStrategy.ExecuteContext buildContext(AiAgent agent, AgentRunRequest request) {
        List<AiAgentFlowConfig> flowConfigs = aiAgentFlowConfigMapper.selectByAgentId(agent.getAgentId());
        if (flowConfigs == null || flowConfigs.isEmpty()) {
            throw new IllegalArgumentException("Agent未配置执行流程: " + agent.getAgentId());
        }

        // 构建 clientType -> clientId 映射（保持插入顺序）
        Map<String, String> clientTypeMap = new LinkedHashMap<>();
        // 构建 clientType -> stepPrompt 映射
        Map<String, String> stepPromptMap = new LinkedHashMap<>();
        for (AiAgentFlowConfig config : flowConfigs) {
            String clientType = config.getClientType() != null ? config.getClientType() : "default";
            // Fixed策略可能有多个default类型, 用sequence区分
            String key = clientType.equals("default") ? clientType + "_" + config.getSequence() : clientType;
            clientTypeMap.put(key, config.getClientId());
            if (config.getStepPrompt() != null && !config.getStepPrompt().isBlank()) {
                stepPromptMap.put(key, config.getStepPrompt());
            }
        }

        String conversationId = request.getConversationId() != null
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        return IExecuteStrategy.ExecuteContext.builder()
                .agentId(agent.getAgentId())
                .userInput(request.getInput())
                .conversationId(conversationId)
                .maxStep(request.getMaxStep() > 0 ? request.getMaxStep() : 3)
                .clientTypeMap(clientTypeMap)
                .stepPromptMap(stepPromptMap)
                .build();
    }

    /**
     * 根据策略名称解析执行策略Bean
     */
    private IExecuteStrategy resolveStrategy(String strategy) {
        String beanName = STRATEGY_BEAN_MAP.getOrDefault(
                strategy != null ? strategy : "fixed", "fixedExecuteStrategy");
        IExecuteStrategy executeStrategy = executeStrategyMap.get(beanName);
        if (executeStrategy == null) {
            throw new IllegalArgumentException("不存在的执行策略: " + strategy);
        }
        return executeStrategy;
    }

}
