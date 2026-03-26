package io.wanjune.zagent.web.agent;

import io.wanjune.zagent.common.Result;
import io.wanjune.zagent.agent.service.AgentService;
import io.wanjune.zagent.agent.model.AgentResultVO;
import io.wanjune.zagent.agent.model.AgentRunRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent编排控制器, 提供Agent同步执行、流式执行和可用Agent查询接口
 *
 * @author zagent
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AgentController {

    @Resource
    private AgentService agentService;

    /**
     * 同步执行Agent流程, POST /api/agent/run
     */
    @PostMapping("/run")
    public Result<AgentResultVO> run(@RequestBody AgentRunRequest request) {
        AgentResultVO result = agentService.run(request);
        return Result.success(result);
    }

    /**
     * SSE流式执行Agent, GET /api/agent/run/stream
     */
    @GetMapping(value = "/run/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runStream(@RequestParam String agentId,
                                 @RequestParam String input,
                                 @RequestParam(required = false) String conversationId,
                                 @RequestParam(required = false, defaultValue = "3") int maxStep) {
        AgentRunRequest request = new AgentRunRequest();
        request.setAgentId(agentId);
        request.setInput(input);
        request.setConversationId(conversationId);
        request.setMaxStep(maxStep);
        return agentService.runStream(request);
    }

}
