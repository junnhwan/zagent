package io.wanjune.zagent.common;

/**
 * 全局常量定义
 *
 * @author zagent
 */
public class Constants {

    // ==================== 通道类型常量 ====================

    /** Agent通道类型 */
    public static final String CHANNEL_AGENT = "agent";
    /** 流式对话通道类型 */
    public static final String CHANNEL_CHAT_STREAM = "chat_stream";

    // ==================== 配置类型常量（对应ConfigTypeEnum） ====================

    /** API配置类型 */
    public static final String TYPE_API = "api";
    /** 模型配置类型 */
    public static final String TYPE_MODEL = "model";
    /** 提示词配置类型 */
    public static final String TYPE_PROMPT = "prompt";
    /** Advisor配置类型 */
    public static final String TYPE_ADVISOR = "advisor";
    /** MCP工具配置类型 */
    public static final String TYPE_TOOL_MCP = "tool_mcp";
    /** 客户端配置类型 */
    public static final String TYPE_CLIENT = "client";

}
