package io.wanjune.zagent.mcp;

import io.wanjune.zagent.common.Constants;
import io.wanjune.zagent.mapper.AiClientConfigMapper;
import io.wanjune.zagent.model.dto.AiClientBindingResolution;
import io.wanjune.zagent.model.entity.AiClientConfig;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class McpBindingResolverImpl implements McpBindingResolver {

    private final AiClientConfigMapper aiClientConfigMapper;

    public McpBindingResolverImpl(AiClientConfigMapper aiClientConfigMapper) {
        this.aiClientConfigMapper = aiClientConfigMapper;
    }

    @Override
    public AiClientBindingResolution resolve(String clientId) {
        List<AiClientConfig> clientConfigs = aiClientConfigMapper.selectBySource(Constants.TYPE_CLIENT, clientId);
        if (clientConfigs == null || clientConfigs.isEmpty()) {
            throw new IllegalStateException("No enabled config found for clientId: " + clientId);
        }

        String modelId = firstTargetId(clientConfigs, Constants.TYPE_MODEL);
        if (modelId == null) {
            throw new IllegalStateException("No enabled model binding found for clientId: " + clientId);
        }

        List<AiClientConfig> modelConfigs = aiClientConfigMapper.selectBySourceAndTargetType(
                Constants.TYPE_MODEL, modelId, Constants.TYPE_TOOL_MCP);

        return new AiClientBindingResolution(
                clientId,
                modelId,
                distinctTargetIds(clientConfigs, Constants.TYPE_PROMPT),
                distinctTargetIds(clientConfigs, Constants.TYPE_ADVISOR),
                distinctTargetIds(modelConfigs, Constants.TYPE_TOOL_MCP)
        );
    }

    private String firstTargetId(List<AiClientConfig> configs, String targetType) {
        return configs.stream()
                .filter(config -> targetType.equals(config.getTargetType()))
                .map(AiClientConfig::getTargetId)
                .findFirst()
                .orElse(null);
    }

    private List<String> distinctTargetIds(List<AiClientConfig> configs, String targetType) {
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> targetIds = new LinkedHashSet<>();
        for (AiClientConfig config : configs) {
            if (targetType.equals(config.getTargetType()) && config.getTargetId() != null) {
                targetIds.add(config.getTargetId());
            }
        }
        return List.copyOf(targetIds);
    }
}
