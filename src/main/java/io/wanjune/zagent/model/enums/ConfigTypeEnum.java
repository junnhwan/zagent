package io.wanjune.zagent.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ai_client_config关联配置类型枚举, 定义source/target的所有合法类型
 *
 * @author zagent
 */
@Getter
@AllArgsConstructor
public enum ConfigTypeEnum {

    /** API配置 */
    API("api"),
    /** 模型配置 */
    MODEL("model"),
    /** 提示词配置 */
    PROMPT("prompt"),
    /** Advisor配置 */
    ADVISOR("advisor"),
    /** MCP工具配置 */
    TOOL_MCP("tool_mcp"),
    /** 客户端配置 */
    CLIENT("client");

    /** 配置类型编码 */
    private final String code;

    /**
     * 根据编码获取枚举实例
     *
     * @param code 配置类型编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static ConfigTypeEnum of(String code) {
        for (ConfigTypeEnum type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown config type: " + code);
    }
}
