package io.wanjune.zagent.model.dto;

import java.util.List;
import java.util.Map;

public record StdioTransportConfig(
        String toolName,
        String command,
        List<String> args,
        Map<String, String> env
) {
}
