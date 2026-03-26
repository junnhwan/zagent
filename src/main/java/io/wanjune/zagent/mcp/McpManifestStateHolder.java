package io.wanjune.zagent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wanjune.zagent.model.dto.McpSyncManifest;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class McpManifestStateHolder {

    private final McpSyncProperties properties;
    private final ObjectMapper objectMapper;
    private McpSyncManifest currentManifest;

    public McpManifestStateHolder(McpSyncProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initialize() {
        resetToConfigured();
    }

    public synchronized McpSyncManifest snapshot() {
        return deepCopy(currentManifest);
    }

    public synchronized void replace(McpSyncManifest manifest) {
        this.currentManifest = deepCopy(manifest);
    }

    public synchronized void resetToConfigured() {
        this.currentManifest = deepCopy(properties.getManifest());
    }

    private McpSyncManifest deepCopy(McpSyncManifest manifest) {
        McpSyncManifest source = manifest == null ? new McpSyncManifest() : manifest;
        return objectMapper.convertValue(source, McpSyncManifest.class);
    }
}
