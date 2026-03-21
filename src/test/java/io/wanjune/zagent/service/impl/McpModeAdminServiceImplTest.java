package io.wanjune.zagent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wanjune.zagent.model.vo.McpModeStatusVO;
import io.wanjune.zagent.service.AiClientAssemblyService;
import io.wanjune.zagent.service.McpConfigSyncService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class McpModeAdminServiceImplTest {

    @Test
    void getCurrentStatusSupportsUtf8BomManifest() throws Exception {
        Path tempFile = Files.createTempFile("mcp-tools-bom", ".json");
        String json = """
                {
                  "models": [],
                  "mcps": [],
                  "bindings": [
                    {
                      "sourceType": "client",
                      "sourceId": "3006",
                      "targetType": "model",
                      "targetIds": ["2002"],
                      "status": 1
                    }
                  ]
                }
                """;

        Files.writeString(tempFile, "\uFEFF" + json, StandardCharsets.UTF_8);

        McpModeAdminServiceImpl service = new McpModeAdminServiceImpl(
                new ObjectMapper(),
                new DefaultResourceLoader(),
                Mockito.mock(McpConfigSyncService.class),
                Mockito.mock(AiClientAssemblyService.class)
        );
        ReflectionTestUtils.setField(service, "configLocation", tempFile.toUri().toString());

        McpModeStatusVO status = service.getCurrentStatus();

        assertThat(status.getCurrentMode()).isEqualTo("stdio");
        assertThat(status.getCurrentModelId()).isEqualTo("2002");
        assertThat(status.getOptions()).hasSize(3);
    }
}
