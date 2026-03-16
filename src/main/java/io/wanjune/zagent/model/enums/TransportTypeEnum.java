package io.wanjune.zagent.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MCP传输类型枚举: SSE(服务端推送事件) / STDIO(标准输入输出)
 *
 * @author zagent
 */
@Getter
@AllArgsConstructor
public enum TransportTypeEnum {

    /** 服务端推送事件 */
    SSE("sse"),
    /** 标准输入输出 */
    STDIO("stdio");

    /** 传输类型编码 */
    private final String code;

    /**
     * 根据编码获取枚举实例
     *
     * @param code 传输类型编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static TransportTypeEnum of(String code) {
        for (TransportTypeEnum type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transport type: " + code);
    }
}
