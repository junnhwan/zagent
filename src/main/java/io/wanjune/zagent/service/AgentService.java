package io.wanjune.zagent.service;

import io.wanjune.zagent.model.dto.AgentRunRequest;
import io.wanjune.zagent.model.vo.AgentResultVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent编排服务接口, 支持多步流程的同步和流式执行。
 */
public interface AgentService {

    /**
     * 同步执行Agent多步流程, 返回所有步骤结果。
     * <p>根据Agent配置的流程步骤依次执行, 收集每个步骤的输出,
     * 最终返回包含所有步骤结果的完整响应。</p>
     *
     * @param request Agent执行请求, 包含agentId、输入内容和可选的conversationId
     * @return 包含所有步骤执行结果的 {@link AgentResultVO}
     */
    AgentResultVO run(AgentRunRequest request);

    /**
     * SSE流式执行Agent。
     * <p>通过Server-Sent Events实时推送Agent流程中最后一步的AI生成内容,
     * 实现流式输出效果。</p>
     *
     * @param request Agent执行请求, 包含agentId、输入内容和可选的conversationId
     * @return SSE发射器 {@link SseEmitter}, 客户端可通过EventSource接收流式数据
     */
    SseEmitter runStream(AgentRunRequest request);

}
