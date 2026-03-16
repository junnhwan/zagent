package io.wanjune.zagent.service;

import org.springframework.ai.chat.client.ChatClient;

/**
 * AI客户端动态装配服务接口, 是整个系统的核心, 负责从数据库配置动态构建Spring AI ChatClient。
 */
public interface AiClientAssemblyService {

    /**
     * 根据clientId获取或构建ChatClient（带缓存）。
     * <p>从数据库加载配置, 构建 OpenAiApi -> ChatModel -> ChatClient（含Advisors和MCP工具）。
     * 已构建的ChatClient会被缓存, 相同clientId的后续调用直接返回缓存实例。</p>
     *
     * @param clientId 客户端标识ID
     * @return 组装完成的 {@link ChatClient} 实例
     */
    ChatClient getOrBuildChatClient(String clientId);

    /**
     * 使指定clientId的缓存失效, 下次访问时强制重建。
     * <p>当数据库中的客户端配置发生变更时, 调用此方法清除旧的缓存实例。</p>
     *
     * @param clientId 需要失效的客户端标识ID
     */
    void invalidate(String clientId);

}
