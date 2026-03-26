package io.wanjune.zagent.mcp;

import io.wanjune.zagent.model.dto.McpSyncManifest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class McpConfigSyncServiceImpl implements McpConfigSyncService {

    private static final String UPSERT_MODEL_SQL = "INSERT INTO ai_client_model " +
            "(model_id, api_id, model_name, model_type, status) VALUES (?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE api_id = VALUES(api_id), model_name = VALUES(model_name), " +
            "model_type = VALUES(model_type), status = VALUES(status)";

    private static final String UPSERT_MCP_SQL = "INSERT INTO ai_client_tool_mcp " +
            "(mcp_id, mcp_name, transport_type, transport_config, request_timeout, status) VALUES (?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE mcp_name = VALUES(mcp_name), transport_type = VALUES(transport_type), " +
            "transport_config = VALUES(transport_config), request_timeout = VALUES(request_timeout), status = VALUES(status)";

    private static final String DELETE_BINDINGS_SQL = "DELETE FROM ai_client_config WHERE source_type = ? AND source_id = ? AND target_type = ?";

    private static final String INSERT_BINDING_SQL = "INSERT INTO ai_client_config " +
            "(source_type, source_id, target_type, target_id, ext_param, status) VALUES (?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final McpSyncProperties properties;
    private final McpManifestStateHolder manifestStateHolder;
    private final Environment environment;
    private final AtomicBoolean synced = new AtomicBoolean(false);

    public McpConfigSyncServiceImpl(@Qualifier("mysqlJdbcTemplate") JdbcTemplate jdbcTemplate,
                                    McpSyncProperties properties,
                                    McpManifestStateHolder manifestStateHolder,
                                    Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.manifestStateHolder = manifestStateHolder;
        this.environment = environment;
    }

    @Override
    public void syncIfEnabled() {
        if (!properties.isEnabled()) {
            log.info("MCP config sync disabled");
            return;
        }
        if (synced.compareAndSet(false, true)) {
            syncNow();
            return;
        }
        log.debug("MCP config sync already completed, skipping repeat sync");
    }

    @Override
    public void forceSync() {
        syncNow();
        synced.set(true);
    }

    void syncNow() {
        McpSyncManifest manifest = loadManifest();
        syncModels(manifest.getModels());
        syncMcps(manifest.getMcps());
        syncBindings(manifest.getBindings());
        log.info("MCP config sync finished: models={}, mcps={}, bindings={}",
                safeList(manifest.getModels()).size(),
                safeList(manifest.getMcps()).size(),
                safeList(manifest.getBindings()).size());
    }

    McpSyncManifest loadManifest() {
        McpSyncManifest manifest = manifestStateHolder.snapshot();
        resolvePlaceholders(manifest);
        validateManifest(manifest);
        return manifest;
    }

    private void resolvePlaceholders(McpSyncManifest manifest) {
        for (McpSyncManifest.ModelConfig model : safeList(manifest.getModels())) {
            model.setModelId(resolve(model.getModelId()));
            model.setApiId(resolve(model.getApiId()));
            model.setModelName(resolve(model.getModelName()));
            model.setModelType(resolve(model.getModelType()));
        }
        for (McpSyncManifest.McpToolConfig mcp : safeList(manifest.getMcps())) {
            mcp.setMcpId(resolve(mcp.getMcpId()));
            mcp.setMcpName(resolve(mcp.getMcpName()));
            mcp.setTransportType(resolve(mcp.getTransportType()));
            mcp.setTransportConfig(resolve(mcp.getTransportConfig()));
        }
        for (McpSyncManifest.BindingConfig binding : safeList(manifest.getBindings())) {
            binding.setSourceType(resolve(binding.getSourceType()));
            binding.setSourceId(resolve(binding.getSourceId()));
            binding.setTargetType(resolve(binding.getTargetType()));
            binding.setExtParam(resolve(binding.getExtParam()));
            if (binding.getTargetIds() != null) {
                binding.setTargetIds(binding.getTargetIds().stream().map(this::resolve).toList());
            }
        }
    }

    private void syncModels(List<McpSyncManifest.ModelConfig> models) {
        for (McpSyncManifest.ModelConfig model : safeList(models)) {
            jdbcTemplate.update(UPSERT_MODEL_SQL,
                    model.getModelId(),
                    model.getApiId(),
                    model.getModelName(),
                    model.getModelType(),
                    defaultStatus(model.getStatus()));
        }
    }

    private void syncMcps(List<McpSyncManifest.McpToolConfig> mcps) {
        for (McpSyncManifest.McpToolConfig mcp : safeList(mcps)) {
            jdbcTemplate.update(UPSERT_MCP_SQL,
                    mcp.getMcpId(),
                    mcp.getMcpName(),
                    mcp.getTransportType(),
                    StringUtils.trimToNull(mcp.getTransportConfig()),
                    defaultTimeout(mcp.getRequestTimeout()),
                    defaultStatus(mcp.getStatus()));
        }
    }

    private void syncBindings(List<McpSyncManifest.BindingConfig> bindings) {
        for (McpSyncManifest.BindingConfig binding : safeList(bindings)) {
            jdbcTemplate.update(DELETE_BINDINGS_SQL,
                    binding.getSourceType(),
                    binding.getSourceId(),
                    binding.getTargetType());

            for (String targetId : distinctTargetIds(binding.getTargetIds())) {
                jdbcTemplate.update(INSERT_BINDING_SQL,
                        binding.getSourceType(),
                        binding.getSourceId(),
                        binding.getTargetType(),
                        targetId,
                        StringUtils.trimToNull(binding.getExtParam()),
                        defaultStatus(binding.getStatus()));
            }
        }
    }

    private void validateManifest(McpSyncManifest manifest) {
        if (manifest == null) {
            throw new IllegalArgumentException("MCP sync config is empty");
        }
        for (McpSyncManifest.ModelConfig model : safeList(manifest.getModels())) {
            require(model.getModelId(), "modelId");
            require(model.getApiId(), "apiId");
            require(model.getModelName(), "modelName");
            require(model.getModelType(), "modelType");
        }
        for (McpSyncManifest.McpToolConfig mcp : safeList(manifest.getMcps())) {
            require(mcp.getMcpId(), "mcpId");
            require(mcp.getMcpName(), "mcpName");
            require(mcp.getTransportType(), "transportType");
        }
        for (McpSyncManifest.BindingConfig binding : safeList(manifest.getBindings())) {
            require(binding.getSourceType(), "sourceType");
            require(binding.getSourceId(), "sourceId");
            require(binding.getTargetType(), "targetType");
            Objects.requireNonNull(binding.getTargetIds(), "targetIds must not be null");
        }
    }

    private void require(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private String resolve(String value) {
        if (value == null) {
            return null;
        }
        return environment.resolvePlaceholders(value);
    }

    private List<String> distinctTargetIds(List<String> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String targetId : targetIds) {
            if (StringUtils.isNotBlank(targetId)) {
                values.add(targetId.trim());
            }
        }
        return new ArrayList<>(values);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private int defaultTimeout(Integer requestTimeout) {
        return requestTimeout == null ? 180 : requestTimeout;
    }

    private int defaultStatus(Integer status) {
        return status == null ? 1 : status;
    }
}
