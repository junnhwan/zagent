package io.wanjune.zagent.service.impl;

import io.modelcontextprotocol.client.transport.ServerParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiClientAssemblyServiceImplTest {

    @Test
    void buildServerParametersSupportsArgsAndEnv() {
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

        ServerParameters params = AiClientAssemblyServiceImpl.buildServerParameters(transportConfig);

        assertThat(params.getCommand()).isEqualTo("npx");
        assertThat(params.getArgs()).containsExactly("-y", "@modelcontextprotocol/server-filesystem", "D:\\dev\\my_proj\\zagent\\docs");
        assertThat(params.getEnv()).containsAllEntriesOf(Map.of(
                "MCP_LOG_LEVEL", "debug",
                "TEST_MODE", "true"
        ));
    }
}
