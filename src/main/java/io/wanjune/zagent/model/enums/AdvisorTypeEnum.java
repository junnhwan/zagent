package io.wanjune.zagent.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spring AI Advisor类型枚举: CHAT_MEMORY(对话记忆) / RAG_ANSWER(RAG检索增强)
 *
 * @author zagent
 */
@Getter
@AllArgsConstructor
public enum AdvisorTypeEnum {

    /** 对话记忆Advisor */
    CHAT_MEMORY("ChatMemory"),
    /** RAG检索增强Advisor */
    RAG_ANSWER("RagAnswer");

    /** Advisor类型编码 */
    private final String code;

    /**
     * 根据编码获取枚举实例
     *
     * @param code Advisor类型编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static AdvisorTypeEnum of(String code) {
        for (AdvisorTypeEnum type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown advisor type: " + code);
    }
}
