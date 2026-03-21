package io.wanjune.zagent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpModeStatusVO {

    private String currentMode;

    private String currentModelId;

    private List<String> managedClientIds;

    private List<McpModeOptionVO> options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpModeOptionVO {
        private String mode;
        private String label;
        private String modelId;
        private String description;
    }
}
