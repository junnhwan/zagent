package io.wanjune.zagent.controller;

import io.wanjune.zagent.common.Result;
import io.wanjune.zagent.mapper.AiAgentFlowConfigMapper;
import io.wanjune.zagent.mapper.AiAgentMapper;
import io.wanjune.zagent.mapper.AiClientMapper;
import io.wanjune.zagent.model.entity.AiAgent;
import io.wanjune.zagent.model.entity.AiAgentFlowConfig;
import io.wanjune.zagent.model.entity.AiClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台控制器, 提供Agent/FlowConfig/Client的CRUD接口
 *
 * @author zagent
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminController {

    @Resource
    private AiAgentMapper aiAgentMapper;
    @Resource
    private AiAgentFlowConfigMapper aiAgentFlowConfigMapper;
    @Resource
    private AiClientMapper aiClientMapper;

    // ==================== Agent CRUD ====================

    @GetMapping("/agents")
    public Result<List<AiAgent>> listAgents() {
        return Result.success(aiAgentMapper.selectAllIncludeDisabled());
    }

    @GetMapping("/agents/{agentId}")
    public Result<AiAgent> getAgent(@PathVariable String agentId) {
        AiAgent agent = aiAgentMapper.selectByAgentId(agentId);
        if (agent == null) {
            return Result.fail("Agent不存在: " + agentId);
        }
        return Result.success(agent);
    }

    @PostMapping("/agents")
    public Result<AiAgent> createAgent(@RequestBody AiAgent agent) {
        if (agent.getAgentId() == null || agent.getAgentId().isBlank()) {
            return Result.fail("agentId不能为空");
        }
        if (aiAgentMapper.selectByAgentId(agent.getAgentId()) != null) {
            return Result.fail("agentId已存在: " + agent.getAgentId());
        }
        aiAgentMapper.insert(agent);
        log.info("创建Agent: {}", agent.getAgentId());
        return Result.success(agent);
    }

    @PutMapping("/agents/{agentId}")
    public Result<Void> updateAgent(@PathVariable String agentId, @RequestBody AiAgent agent) {
        agent.setAgentId(agentId);
        int rows = aiAgentMapper.update(agent);
        if (rows == 0) {
            return Result.fail("Agent不存在: " + agentId);
        }
        log.info("更新Agent: {}", agentId);
        return Result.success();
    }

    @DeleteMapping("/agents/{agentId}")
    public Result<Void> deleteAgent(@PathVariable String agentId) {
        aiAgentMapper.deleteByAgentId(agentId);
        aiAgentFlowConfigMapper.deleteByAgentId(agentId);
        log.info("删除Agent及其FlowConfig: {}", agentId);
        return Result.success();
    }

    // ==================== FlowConfig CRUD ====================

    @GetMapping("/agents/{agentId}/flows")
    public Result<List<AiAgentFlowConfig>> listFlowConfigs(@PathVariable String agentId) {
        return Result.success(aiAgentFlowConfigMapper.selectByAgentId(agentId));
    }

    @PostMapping("/agents/{agentId}/flows")
    public Result<AiAgentFlowConfig> createFlowConfig(@PathVariable String agentId, @RequestBody AiAgentFlowConfig config) {
        config.setAgentId(agentId);
        if (config.getClientId() == null || config.getClientId().isBlank()) {
            return Result.fail("clientId不能为空");
        }
        aiAgentFlowConfigMapper.insert(config);
        log.info("创建FlowConfig: agentId={}, clientId={}", agentId, config.getClientId());
        return Result.success(config);
    }

    @PutMapping("/flows/{id}")
    public Result<Void> updateFlowConfig(@PathVariable Long id, @RequestBody AiAgentFlowConfig config) {
        config.setId(id);
        int rows = aiAgentFlowConfigMapper.update(config);
        if (rows == 0) {
            return Result.fail("FlowConfig不存在: id=" + id);
        }
        log.info("更新FlowConfig: id={}", id);
        return Result.success();
    }

    @DeleteMapping("/flows/{id}")
    public Result<Void> deleteFlowConfig(@PathVariable Long id) {
        aiAgentFlowConfigMapper.deleteById(id);
        log.info("删除FlowConfig: id={}", id);
        return Result.success();
    }

    // ==================== Client 只读列表 ====================

    @GetMapping("/clients")
    public Result<List<AiClient>> listClients() {
        return Result.success(aiClientMapper.selectAll());
    }

}
