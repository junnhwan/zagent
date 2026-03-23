package io.wanjune.zagent.mcp;

public interface McpConfigSyncService {

    void syncIfEnabled();

    void forceSync();
}
