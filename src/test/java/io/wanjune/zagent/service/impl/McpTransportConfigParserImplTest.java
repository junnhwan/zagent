package io.wanjune.zagent.service.impl;

import io.modelcontextprotocol.client.transport.ServerParameters;
import io.wanjune.zagent.model.dto.SseTransportConfig;
import io.wanjune.zagent.model.dto.StdioTransportConfig;
import io.wanjune.zagent.service.McpTransportConfigParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpTransportConfigParserImplTest {

    private final McpTransportConfigParser parser = new McpTransportConfigParserImpl();

    @Test
    void parseSseConfigNormalizesBaseUriAndEndpointSlashes() {
        SseTransportConfig config = parser.parseSse("""
                {
                  "baseUri": "http://127.0.0.1:18080/",
                  "sseEndpoint": "sse/"
                }
                """);

        assertThat(config.baseUri()).isEqualTo("http://127.0.0.1:18080");
        assertThat(config.sseEndpoint()).isEqualTo("/sse");
    }

    @Test
    void parseStdioConfigSupportsArgsEnvAndToolName() {
        StdioTransportConfig config = parser.parseStdio("""
                {
                  "filesystem": {
                    "command": "npx",
                    "args": ["-y", "@modelcontextprotocol/server-filesystem", "D:\\\\dev\\\\my_proj\\\\zagent\\\\docs"],
                    "env": {
                      "MCP_LOG_LEVEL": "debug",
                      "TEST_MODE": "true"
                    }
                  }
                }
                """);

        assertThat(config.toolName()).isEqualTo("filesystem");
        assertThat(config.command()).isEqualTo("npx");
        assertThat(config.args()).containsExactly("-y", "@modelcontextprotocol/server-filesystem", "D:\\dev\\my_proj\\zagent\\docs");
        assertThat(config.env()).containsAllEntriesOf(Map.of(
                "MCP_LOG_LEVEL", "debug",
                "TEST_MODE", "true"
        ));

        ServerParameters params = parser.toServerParameters(config);
        assertThat(params.getCommand()).isEqualTo("npx");
        assertThat(params.getArgs()).containsExactly("-y", "@modelcontextprotocol/server-filesystem", "D:\\dev\\my_proj\\zagent\\docs");
        assertThat(params.getEnv()).containsAllEntriesOf(config.env());
    }
}
