package io.wanjune.zagent.mcp;

import io.wanjune.zagent.model.dto.McpSyncManifest;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zagent.mcp.sync")
public class McpSyncProperties {

    private boolean enabled = true;

    private McpSyncManifest manifest = new McpSyncManifest();
}
