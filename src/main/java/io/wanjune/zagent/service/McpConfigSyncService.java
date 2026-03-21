package io.wanjune.zagent.service;

public interface McpConfigSyncService {

    void syncIfEnabled();

    void forceSync();
}
