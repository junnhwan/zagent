package io.wanjune.zagent.chat.assembly.impl;

import io.wanjune.zagent.chat.assembly.factory.AiClientMcpToolFactory;
import io.wanjune.zagent.mcp.impl.McpTransportConfigParserImpl;
import io.wanjune.zagent.model.dto.StdioTransportConfig;
import io.wanjune.zagent.model.entity.AiClientToolMcp;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiClientAssemblyServiceImplTest {

    private final McpTransportConfigParserImpl parser = new McpTransportConfigParserImpl();

    @Test
    void parseStdioSupportsArgsAndEnv() {
        String transportConfig = """
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
                """;

        StdioTransportConfig config = parser.parseStdio(transportConfig);

        assertThat(config.command()).isEqualTo("npx");
        assertThat(config.args()).containsExactly("-y", "@modelcontextprotocol/server-filesystem", "D:\\dev\\my_proj\\zagent\\docs");
        assertThat(config.env()).containsAllEntriesOf(Map.of(
                "MCP_LOG_LEVEL", "debug",
                "TEST_MODE", "true"
        ));
    }

    @Test
    void normalizeSseConfigNormalizesBaseUriAndEndpointSlashes() {
        assertThat(McpTransportConfigParserImpl.normalizeSseBaseUri("http://127.0.0.1:18080/"))
                .isEqualTo("http://127.0.0.1:18080");

        assertThat(McpTransportConfigParserImpl.normalizeSseEndpoint("/sse"))
                .isEqualTo("/sse");

        assertThat(McpTransportConfigParserImpl.normalizeSseEndpoint("sse/"))
                .isEqualTo("/sse");

        assertThat(McpTransportConfigParserImpl.normalizeSseEndpoint(null))
                .isEqualTo("/sse");
    }

    @Test
    void formatMcpBindingLabelIncludesNameAndTransport() {
        AiClientToolMcp mcp = new AiClientToolMcp();
        mcp.setMcpName("filesystem-docs");
        mcp.setTransportType("stdio");

        assertThat(AiClientMcpToolFactory.formatMcpBindingLabel(mcp))
                .isEqualTo("filesystem-docs[stdio]");
    }
}
