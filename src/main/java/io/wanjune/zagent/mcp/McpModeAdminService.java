package io.wanjune.zagent.mcp;

import io.wanjune.zagent.model.vo.McpModeStatusVO;

public interface McpModeAdminService {

    McpModeStatusVO getCurrentStatus();

    McpModeStatusVO switchMode(String mode);
}
