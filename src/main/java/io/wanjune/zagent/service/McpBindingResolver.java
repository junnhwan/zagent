package io.wanjune.zagent.service;

import io.wanjune.zagent.model.dto.AiClientBindingResolution;

public interface McpBindingResolver {

    AiClientBindingResolution resolve(String clientId);
}
