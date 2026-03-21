package io.wanjune.zagent.model.dto;

import java.time.Instant;

public record McpRuntimeState(
        boolean initialized,
        String lastError,
        Instant updatedAt
) {
}
