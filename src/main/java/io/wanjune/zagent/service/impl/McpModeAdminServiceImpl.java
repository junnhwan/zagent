package io.wanjune.zagent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wanjune.zagent.model.dto.McpSyncManifest;
import io.wanjune.zagent.model.vo.McpModeStatusVO;
import io.wanjune.zagent.service.AiClientAssemblyService;
import io.wanjune.zagent.service.McpConfigSyncService;
import io.wanjune.zagent.service.McpModeAdminService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class McpModeAdminServiceImpl implements McpModeAdminService {

    private static final List<String> MANAGED_CLIENT_IDS = List.of("3006", "3008");
    private static final Map<String, ModeDefinition> MODE_DEFINITIONS = new LinkedHashMap<>();

    static {
        MODE_DEFINITIONS.put("stdio", new ModeDefinition("stdio", "STDIO Filesystem", "2002", "本地 filesystem MCP，适合读取 docs 目录文件"));
        MODE_DEFINITIONS.put("sse_probe", new ModeDefinition("sse_probe", "SSE Probe", "2003", "本地 SSE probe MCP，适合验证 SSE 接入"));
        MODE_DEFINITIONS.put("amap", new ModeDefinition("amap", "高德天气/POI", "2005", "高德 SSE MCP，支持天气和 POI 搜索"));
    }

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final McpConfigSyncService mcpConfigSyncService;
    private final AiClientAssemblyService aiClientAssemblyService;

    @Value("${zagent.mcp.sync.location:classpath:mcp-tools.json}")
    private String configLocation;

    public McpModeAdminServiceImpl(ObjectMapper objectMapper,
                                   ResourceLoader resourceLoader,
                                   McpConfigSyncService mcpConfigSyncService,
                                   AiClientAssemblyService aiClientAssemblyService) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.mcpConfigSyncService = mcpConfigSyncService;
        this.aiClientAssemblyService = aiClientAssemblyService;
    }

    @Override
    public McpModeStatusVO getCurrentStatus() {
        McpSyncManifest manifest = loadManifest();
        String modelId = resolveManagedModelId(manifest);
        return buildStatus(modelId);
    }

    @Override
    public McpModeStatusVO switchMode(String mode) {
        ModeDefinition modeDefinition = MODE_DEFINITIONS.get(normalizeMode(mode));
        if (modeDefinition == null) {
            throw new IllegalArgumentException("Unsupported MCP mode: " + mode);
        }

        McpSyncManifest manifest = loadManifest();
        for (McpSyncManifest.BindingConfig binding : safeList(manifest.getBindings())) {
            if ("client".equals(binding.getSourceType()) && MANAGED_CLIENT_IDS.contains(binding.getSourceId())
                    && "model".equals(binding.getTargetType())) {
                binding.setTargetIds(List.of(modeDefinition.modelId()));
                binding.setStatus(1);
            }
        }

        saveManifest(manifest);
        mcpConfigSyncService.forceSync();
        for (String clientId : MANAGED_CLIENT_IDS) {
            aiClientAssemblyService.invalidate(clientId);
        }
        return buildStatus(modeDefinition.modelId());
    }

    private McpModeStatusVO buildStatus(String modelId) {
        String currentMode = MODE_DEFINITIONS.values().stream()
                .filter(def -> Objects.equals(def.modelId(), modelId))
                .map(ModeDefinition::mode)
                .findFirst()
                .orElse("unknown");

        List<McpModeStatusVO.McpModeOptionVO> options = MODE_DEFINITIONS.values().stream()
                .map(def -> McpModeStatusVO.McpModeOptionVO.builder()
                        .mode(def.mode())
                        .label(def.label())
                        .modelId(def.modelId())
                        .description(def.description())
                        .build())
                .toList();

        return McpModeStatusVO.builder()
                .currentMode(currentMode)
                .currentModelId(modelId)
                .managedClientIds(MANAGED_CLIENT_IDS)
                .options(options)
                .build();
    }

    private String resolveManagedModelId(McpSyncManifest manifest) {
        return safeList(manifest.getBindings()).stream()
                .filter(binding -> "client".equals(binding.getSourceType()))
                .filter(binding -> MANAGED_CLIENT_IDS.contains(binding.getSourceId()))
                .filter(binding -> "model".equals(binding.getTargetType()))
                .map(McpSyncManifest.BindingConfig::getTargetIds)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .findFirst()
                .orElse(null);
    }

    private McpSyncManifest loadManifest() {
        try {
            String rawJson = Files.readString(resolveWritableConfigPath(), StandardCharsets.UTF_8);
            return objectMapper.readValue(stripUtf8Bom(rawJson), McpSyncManifest.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read MCP config file", e);
        }
    }

    private void saveManifest(McpSyncManifest manifest) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest);
            Files.writeString(resolveWritableConfigPath(), json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write MCP config file", e);
        }
    }

    private Path resolveWritableConfigPath() {
        try {
            if (StringUtils.startsWith(configLocation, "file:")) {
                return Paths.get(URI.create(configLocation));
            }
            if (StringUtils.startsWith(configLocation, "classpath:")) {
                String relativePath = StringUtils.removeStart(configLocation, "classpath:");
                Path sourcePath = Paths.get("src", "main", "resources", relativePath);
                if (Files.exists(sourcePath)) {
                    return sourcePath;
                }
            }
            Resource resource = resourceLoader.getResource(configLocation);
            return resource.getFile().toPath();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot resolve writable MCP config path from: " + configLocation, e);
        }
    }

    private String normalizeMode(String mode) {
        return StringUtils.trimToEmpty(mode).toLowerCase();
    }

    private String stripUtf8Bom(String content) {
        if (content != null && !content.isEmpty() && content.charAt(0) == '\uFEFF') {
            return content.substring(1);
        }
        return content;
    }

    private List<McpSyncManifest.BindingConfig> safeList(List<McpSyncManifest.BindingConfig> bindings) {
        return bindings == null ? List.of() : bindings;
    }

    private record ModeDefinition(String mode, String label, String modelId, String description) {
    }
}
