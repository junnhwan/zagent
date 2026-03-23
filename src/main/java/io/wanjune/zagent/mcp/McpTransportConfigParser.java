package io.wanjune.zagent.mcp;

import io.modelcontextprotocol.client.transport.ServerParameters;
import io.wanjune.zagent.model.dto.SseTransportConfig;
import io.wanjune.zagent.model.dto.StdioTransportConfig;

public interface McpTransportConfigParser {

    SseTransportConfig parseSse(String transportConfig);

    StdioTransportConfig parseStdio(String transportConfig);

    ServerParameters toServerParameters(StdioTransportConfig config);
}
