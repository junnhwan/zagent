package io.wanjune.zagent.mcp;

import io.wanjune.zagent.model.dto.AiClientBindingResolution;

public interface McpBindingResolver {

    AiClientBindingResolution resolve(String clientId);
}
