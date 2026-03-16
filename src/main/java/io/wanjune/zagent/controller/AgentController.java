package io.wanjune.zagent.controller;

import io.wanjune.zagent.common.Result;
import io.wanjune.zagent.model.dto.AgentRunRequest;
import io.wanjune.zagent.model.vo.AgentResultVO;
import io.wanjune.zagent.service.AgentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent编排控制器, 提供Agent执行和流式执行接口
 *
 * @author zagent
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Resource
    private AgentService agentService;

    /**
     * 同步执行Agent流程, POST /api/agent/run
     *
     * @param request Agent执行请求参数
     * @return 包含Agent执行结果的统一响应
     */
    @PostMapping("/run")
    public Result<AgentResultVO> run(@RequestBody AgentRunRequest request) {
        try {
            AgentResultVO result = agentService.run(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Agent run error", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * SSE流式执行Agent, GET /api/agent/run/stream
     *
     * @param agentId        要执行的智能体ID
     * @param input          用户输入/初始消息
     * @param conversationId 对话ID（可选）, 用于对话记忆
     * @return SSE事件发射器, 实时推送Agent执行过程
     */
    @GetMapping(value = "/run/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runStream(@RequestParam String agentId,
                                 @RequestParam String input,
                                 @RequestParam(required = false) String conversationId) {
        AgentRunRequest request = new AgentRunRequest();
        request.setAgentId(agentId);
        request.setInput(input);
        request.setConversationId(conversationId);
        return agentService.runStream(request);
    }

}
