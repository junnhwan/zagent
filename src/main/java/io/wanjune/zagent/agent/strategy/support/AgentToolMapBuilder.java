package io.wanjune.zagent.agent.strategy.support;

import io.wanjune.zagent.agent.tool.AgentToolRegistry;
import io.wanjune.zagent.model.enums.ClientTypeEnum;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentToolMapBuilder {

    private final AgentToolRegistry agentToolRegistry;

    public AgentToolMapBuilder(AgentToolRegistry agentToolRegistry) {
        this.agentToolRegistry = agentToolRegistry;
    }

    public Map<String, ToolCallback> buildAgentToolMap(Map<String, String> clientTypeMap, String currentAgentId) {
        String agentToolConfig = clientTypeMap.get(ClientTypeEnum.AGENT_TOOL.getCode());
        if (agentToolConfig == null || agentToolConfig.isBlank()) {
            return Map.of();
        }

        List<ToolCallback> agentTools;
        if ("all".equalsIgnoreCase(agentToolConfig.trim())) {
            agentTools = agentToolRegistry.buildAllAgentTools(currentAgentId);
        } else {
            List<String> agentIds = Arrays.stream(agentToolConfig.split(","))
                    .map(String::trim)
                    .filter(id -> !id.isBlank() && !id.equals(currentAgentId))
                    .toList();
            agentTools = agentToolRegistry.buildAgentTools(agentIds);
        }
        return buildToolMap(agentTools);
    }

    public Map<String, ToolCallback> buildToolMap(List<ToolCallback> callbacks) {
        Map<String, ToolCallback> toolMap = new HashMap<>();
        if (callbacks == null) {
            return toolMap;
        }
        for (ToolCallback callback : callbacks) {
            if (callback == null || callback.getToolDefinition() == null) {
                continue;
            }
            String name = callback.getToolDefinition().name();
            if (name != null && !name.isBlank()) {
                toolMap.put(name, callback);
            }
        }
        return toolMap;
    }
}
