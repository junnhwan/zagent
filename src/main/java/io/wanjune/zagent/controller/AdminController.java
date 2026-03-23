package io.wanjune.zagent.controller;

import io.wanjune.zagent.agent.tool.AgentToolRegistry;
import io.wanjune.zagent.chat.assembly.AiClientAssemblyService;
import io.wanjune.zagent.common.Result;
import io.wanjune.zagent.model.dto.McpModeSwitchRequest;
import io.wanjune.zagent.model.dto.McpRuntimeState;
import io.wanjune.zagent.mapper.*;
import io.wanjune.zagent.model.entity.*;
import io.wanjune.zagent.model.vo.McpModeStatusVO;
import io.wanjune.zagent.mcp.McpModeAdminService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台控制器, 提供Agent/FlowConfig/Client等全部实体的CRUD接口
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
    @Resource
    private AiClientApiMapper aiClientApiMapper;
    @Resource
    private AiClientModelMapper aiClientModelMapper;
    @Resource
    private AiClientSystemPromptMapper aiClientSystemPromptMapper;
    @Resource
    private AiClientAdvisorMapper aiClientAdvisorMapper;
    @Resource
    private AiClientToolMcpMapper aiClientToolMcpMapper;
    @Resource
    private AiClientConfigMapper aiClientConfigMapper;
    @Resource
    private McpModeAdminService mcpModeAdminService;
    @Resource
    private AgentToolRegistry agentToolRegistry;
    @Resource
    private AiClientAssemblyService aiClientAssemblyService;

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

    // ==================== Client CRUD ====================

    @GetMapping("/clients")
    public Result<List<AiClient>> listClients() {
        return Result.success(aiClientMapper.selectAll());
    }

    @GetMapping("/clients/{clientId}")
    public Result<AiClient> getClient(@PathVariable String clientId) {
        AiClient client = aiClientMapper.selectByClientId(clientId);
        if (client == null) {
            return Result.fail("Client不存在: " + clientId);
        }
        return Result.success(client);
    }

    @PostMapping("/clients")
    public Result<AiClient> createClient(@RequestBody AiClient client) {
        if (client.getClientId() == null || client.getClientId().isBlank()) {
            return Result.fail("clientId不能为空");
        }
        if (aiClientMapper.selectByClientId(client.getClientId()) != null) {
            return Result.fail("clientId已存在: " + client.getClientId());
        }
        aiClientMapper.insert(client);
        log.info("创建Client: {}", client.getClientId());
        return Result.success(client);
    }

    @PutMapping("/clients/{clientId}")
    public Result<Void> updateClient(@PathVariable String clientId, @RequestBody AiClient client) {
        client.setClientId(clientId);
        int rows = aiClientMapper.update(client);
        if (rows == 0) {
            return Result.fail("Client不存在: " + clientId);
        }
        log.info("更新Client: {}", clientId);
        return Result.success();
    }

    @DeleteMapping("/clients/{clientId}")
    public Result<Void> deleteClient(@PathVariable String clientId) {
        aiClientMapper.deleteByClientId(clientId);
        log.info("删除Client: {}", clientId);
        return Result.success();
    }

    // ==================== API CRUD ====================

    @GetMapping("/apis")
    public Result<List<AiClientApi>> listApis() {
        return Result.success(aiClientApiMapper.selectAll());
    }

    @GetMapping("/apis/{apiId}")
    public Result<AiClientApi> getApi(@PathVariable String apiId) {
        AiClientApi api = aiClientApiMapper.selectByApiId(apiId);
        if (api == null) {
            return Result.fail("API不存在: " + apiId);
        }
        return Result.success(api);
    }

    @PostMapping("/apis")
    public Result<AiClientApi> createApi(@RequestBody AiClientApi api) {
        if (api.getApiId() == null || api.getApiId().isBlank()) {
            return Result.fail("apiId不能为空");
        }
        if (aiClientApiMapper.selectByApiId(api.getApiId()) != null) {
            return Result.fail("apiId已存在: " + api.getApiId());
        }
        aiClientApiMapper.insert(api);
        log.info("创建API: {}", api.getApiId());
        return Result.success(api);
    }

    @PutMapping("/apis/{apiId}")
    public Result<Void> updateApi(@PathVariable String apiId, @RequestBody AiClientApi api) {
        api.setApiId(apiId);
        int rows = aiClientApiMapper.update(api);
        if (rows == 0) {
            return Result.fail("API不存在: " + apiId);
        }
        log.info("更新API: {}", apiId);
        return Result.success();
    }

    @DeleteMapping("/apis/{apiId}")
    public Result<Void> deleteApi(@PathVariable String apiId) {
        aiClientApiMapper.deleteByApiId(apiId);
        log.info("删除API: {}", apiId);
        return Result.success();
    }

    // ==================== Model CRUD ====================

    @GetMapping("/models")
    public Result<List<AiClientModel>> listModels() {
        return Result.success(aiClientModelMapper.selectAll());
    }

    @PostMapping("/models")
    public Result<AiClientModel> createModel(@RequestBody AiClientModel model) {
        if (model.getModelId() == null || model.getModelId().isBlank()) {
            return Result.fail("modelId不能为空");
        }
        if (aiClientModelMapper.selectByModelId(model.getModelId()) != null) {
            return Result.fail("modelId已存在: " + model.getModelId());
        }
        aiClientModelMapper.insert(model);
        log.info("创建Model: {}", model.getModelId());
        return Result.success(model);
    }

    @PutMapping("/models/{modelId}")
    public Result<Void> updateModel(@PathVariable String modelId, @RequestBody AiClientModel model) {
        model.setModelId(modelId);
        int rows = aiClientModelMapper.update(model);
        if (rows == 0) {
            return Result.fail("Model不存在: " + modelId);
        }
        log.info("更新Model: {}", modelId);
        return Result.success();
    }

    @DeleteMapping("/models/{modelId}")
    public Result<Void> deleteModel(@PathVariable String modelId) {
        aiClientModelMapper.deleteByModelId(modelId);
        log.info("删除Model: {}", modelId);
        return Result.success();
    }

    // ==================== Prompt CRUD ====================

    @GetMapping("/prompts")
    public Result<List<AiClientSystemPrompt>> listPrompts() {
        return Result.success(aiClientSystemPromptMapper.selectAll());
    }

    @PostMapping("/prompts")
    public Result<AiClientSystemPrompt> createPrompt(@RequestBody AiClientSystemPrompt prompt) {
        if (prompt.getPromptId() == null || prompt.getPromptId().isBlank()) {
            return Result.fail("promptId不能为空");
        }
        if (aiClientSystemPromptMapper.selectByPromptId(prompt.getPromptId()) != null) {
            return Result.fail("promptId已存在: " + prompt.getPromptId());
        }
        aiClientSystemPromptMapper.insert(prompt);
        log.info("创建Prompt: {}", prompt.getPromptId());
        return Result.success(prompt);
    }

    @PutMapping("/prompts/{promptId}")
    public Result<Void> updatePrompt(@PathVariable String promptId, @RequestBody AiClientSystemPrompt prompt) {
        prompt.setPromptId(promptId);
        int rows = aiClientSystemPromptMapper.update(prompt);
        if (rows == 0) {
            return Result.fail("Prompt不存在: " + promptId);
        }
        log.info("更新Prompt: {}", promptId);
        return Result.success();
    }

    @DeleteMapping("/prompts/{promptId}")
    public Result<Void> deletePrompt(@PathVariable String promptId) {
        aiClientSystemPromptMapper.deleteByPromptId(promptId);
        log.info("删除Prompt: {}", promptId);
        return Result.success();
    }

    // ==================== Advisor CRUD ====================

    @GetMapping("/advisors")
    public Result<List<AiClientAdvisor>> listAdvisors() {
        return Result.success(aiClientAdvisorMapper.selectAll());
    }

    @PostMapping("/advisors")
    public Result<AiClientAdvisor> createAdvisor(@RequestBody AiClientAdvisor advisor) {
        if (advisor.getAdvisorId() == null || advisor.getAdvisorId().isBlank()) {
            return Result.fail("advisorId不能为空");
        }
        if (aiClientAdvisorMapper.selectByAdvisorId(advisor.getAdvisorId()) != null) {
            return Result.fail("advisorId已存在: " + advisor.getAdvisorId());
        }
        aiClientAdvisorMapper.insert(advisor);
        log.info("创建Advisor: {}", advisor.getAdvisorId());
        return Result.success(advisor);
    }

    @PutMapping("/advisors/{advisorId}")
    public Result<Void> updateAdvisor(@PathVariable String advisorId, @RequestBody AiClientAdvisor advisor) {
        advisor.setAdvisorId(advisorId);
        int rows = aiClientAdvisorMapper.update(advisor);
        if (rows == 0) {
            return Result.fail("Advisor不存在: " + advisorId);
        }
        log.info("更新Advisor: {}", advisorId);
        return Result.success();
    }

    @DeleteMapping("/advisors/{advisorId}")
    public Result<Void> deleteAdvisor(@PathVariable String advisorId) {
        aiClientAdvisorMapper.deleteByAdvisorId(advisorId);
        log.info("删除Advisor: {}", advisorId);
        return Result.success();
    }

    // ==================== MCP Tool CRUD ====================

    @GetMapping("/mcps")
    public Result<List<AiClientToolMcp>> listMcps() {
        return Result.success(aiClientToolMcpMapper.selectAll());
    }

    @PostMapping("/mcps")
    public Result<AiClientToolMcp> createMcp(@RequestBody AiClientToolMcp mcp) {
        if (mcp.getMcpId() == null || mcp.getMcpId().isBlank()) {
            return Result.fail("mcpId不能为空");
        }
        if (aiClientToolMcpMapper.selectByMcpId(mcp.getMcpId()) != null) {
            return Result.fail("mcpId已存在: " + mcp.getMcpId());
        }
        aiClientToolMcpMapper.insert(mcp);
        log.info("创建MCP: {}", mcp.getMcpId());
        return Result.success(mcp);
    }

    @PutMapping("/mcps/{mcpId}")
    public Result<Void> updateMcp(@PathVariable String mcpId, @RequestBody AiClientToolMcp mcp) {
        mcp.setMcpId(mcpId);
        int rows = aiClientToolMcpMapper.update(mcp);
        if (rows == 0) {
            return Result.fail("MCP不存在: " + mcpId);
        }
        log.info("更新MCP: {}", mcpId);
        return Result.success();
    }

    @DeleteMapping("/mcps/{mcpId}")
    public Result<Void> deleteMcp(@PathVariable String mcpId) {
        aiClientToolMcpMapper.deleteByMcpId(mcpId);
        log.info("删除MCP: {}", mcpId);
        return Result.success();
    }

    @GetMapping("/mcps/runtime-status")
    public Result<Map<String, McpRuntimeState>> getMcpRuntimeStatus() {
        return Result.success(aiClientAssemblyService.getMcpRuntimeStates());
    }

    // ==================== Config Binding CRUD ====================

    @GetMapping("/configs")
    public Result<List<AiClientConfig>> listConfigs() {
        return Result.success(aiClientConfigMapper.selectAll());
    }

    @GetMapping("/configs/client/{clientId}")
    public Result<List<AiClientConfig>> listConfigsByClient(@PathVariable String clientId) {
        return Result.success(aiClientConfigMapper.selectBySource("client", clientId));
    }

    @PostMapping("/configs")
    public Result<AiClientConfig> createConfig(@RequestBody AiClientConfig config) {
        if (config.getSourceType() == null || config.getSourceType().isBlank()) {
            return Result.fail("sourceType不能为空");
        }
        if (config.getSourceId() == null || config.getSourceId().isBlank()) {
            return Result.fail("sourceId不能为空");
        }
        if (config.getTargetType() == null || config.getTargetType().isBlank()) {
            return Result.fail("targetType不能为空");
        }
        if (config.getTargetId() == null || config.getTargetId().isBlank()) {
            return Result.fail("targetId不能为空");
        }
        aiClientConfigMapper.insert(config);
        log.info("创建Config绑定: {}:{} -> {}:{}", config.getSourceType(), config.getSourceId(), config.getTargetType(), config.getTargetId());
        return Result.success(config);
    }

    @DeleteMapping("/configs/{id}")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        aiClientConfigMapper.deleteById(id);
        log.info("删除Config绑定: id={}", id);
        return Result.success();
    }

    // ==================== Cache Management ====================

    @PostMapping("/cache/invalidate/{clientId}")
    public Result<Void> invalidateClientCache(@PathVariable String clientId) {
        aiClientAssemblyService.invalidate(clientId);
        log.info("缓存已失效: {}", clientId);
        return Result.success();
    }

    @PostMapping("/cache/invalidate-all")
    public Result<Void> invalidateAllCache() {
        aiClientAssemblyService.warmUpAll();
        log.info("全部缓存已重建");
        return Result.success();
    }

    // ==================== MCP Config ====================

    @GetMapping("/mcp-config/current")
    public Result<McpModeStatusVO> currentMcpConfig() {
        return Result.success(mcpModeAdminService.getCurrentStatus());
    }

    @PostMapping("/mcp-config/switch")
    public Result<McpModeStatusVO> switchMcpConfig(@RequestBody McpModeSwitchRequest request) {
        if (request == null || request.getMode() == null || request.getMode().isBlank()) {
            return Result.fail("mode 不能为空");
        }
        return Result.success(mcpModeAdminService.switchMode(request.getMode()));
    }

    // ==================== Agent-as-Tool ====================

    @GetMapping("/agent-tools")
    public Result<List<AgentToolRegistry.AgentToolInfo>> listAgentTools() {
        return Result.success(agentToolRegistry.listAgentToolInfos());
    }

}
