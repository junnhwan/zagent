package io.wanjune.zagent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpRuntimeStatusVO {

    private String mcpId;

    private String mcpName;

    private String transportType;

    private String transportSummary;

    private Boolean ready;

    private String lastError;

    private String updatedAt;
}
