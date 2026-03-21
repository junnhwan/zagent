package io.wanjune.zagent.model.dto;

import java.util.List;

public record AiClientBindingResolution(
        String clientId,
        String modelId,
        List<String> promptIds,
        List<String> advisorIds,
        List<String> mcpIds
) {
}
