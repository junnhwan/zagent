package io.wanjune.zagent.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class McpSyncManifest {

    private List<ModelConfig> models = new ArrayList<>();

    private List<McpToolConfig> mcps = new ArrayList<>();

    private List<BindingConfig> bindings = new ArrayList<>();

    @Data
    public static class ModelConfig {
        private String modelId;
        private String apiId;
        private String modelName;
        private String modelType;
        private Integer status = 1;
    }

    @Data
    public static class McpToolConfig {
        private String mcpId;
        private String mcpName;
        private String transportType;
        private String transportConfig;
        private Integer requestTimeout = 180;
        private Integer status = 1;
    }

    @Data
    public static class BindingConfig {
        private String sourceType;
        private String sourceId;
        private String targetType;
        private List<String> targetIds;
        private String extParam;
        private Integer status = 1;
    }
}
