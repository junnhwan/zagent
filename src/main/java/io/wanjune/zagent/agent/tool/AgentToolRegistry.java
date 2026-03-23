package io.wanjune.zagent.agent.tool;

import io.wanjune.zagent.agent.service.AgentService;
import io.wanjune.zagent.mapper.AiAgentMapper;
import io.wanjune.zagent.model.entity.AiAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent工具注册中心, 负责将Agent包装为ToolCallback供其他Agent调用。
 * <p>提供两种构建方式:
 * <ul>
 *   <li>指定agentId列表构建 — 精确控制哪些Agent可作为工具</li>
 *   <li>构建全部启用的Agent — 排除调用方自身以防止无限递归</li>
 * </ul>
 * </p>
 * <p>内置递归保护: 通过ThreadLocal追踪调用链, 防止Agent A调用Agent B再调用Agent A的死循环。</p>
 *
 * @author zagent
 */
@Slf4j
@Service
public class AgentToolRegistry {

    @Resource
    private AiAgentMapper aiAgentMapper;
    @Lazy
    @Resource
    private AgentService agentService;

    /**
     * ThreadLocal调用栈, 用于检测和防止Agent递归调用。
     * 每个线程维护一个当前正在执行的agentId集合。
     */
    private static final ThreadLocal<Set<String>> CALL_STACK = ThreadLocal.withInitial(ConcurrentHashMap::newKeySet);

    /**
     * 根据指定的agentId列表构建ToolCallback。
     *
     * @param agentIds 要作为工具的agentId列表
     * @return 对应的ToolCallback列表, 无效agentId将被跳过
     */
    public List<ToolCallback> buildAgentTools(List<String> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ToolCallback> callbacks = new ArrayList<>();
        for (String agentId : agentIds) {
            AiAgent agent = aiAgentMapper.selectByAgentId(agentId);
            if (agent == null) {
                log.warn("Agent-as-Tool: agentId={} 不存在或已禁用, 跳过", agentId);
                continue;
            }
            callbacks.add(createCallback(agent));
        }
        log.info("Agent-as-Tool: 从指定列表构建了 {} 个Agent工具", callbacks.size());
        return callbacks;
    }

    /**
     * 构建所有启用Agent的ToolCallback, 排除调用方自身以防止递归。
     *
     * @param excludeAgentId 要排除的agentId（通常是调用方自身）
     * @return 除自身外所有启用Agent的ToolCallback列表
     */
    public List<ToolCallback> buildAllAgentTools(String excludeAgentId) {
        List<AiAgent> allAgents = aiAgentMapper.selectAll();
        if (allAgents == null || allAgents.isEmpty()) {
            return Collections.emptyList();
        }
        List<ToolCallback> callbacks = new ArrayList<>();
        for (AiAgent agent : allAgents) {
            if (agent.getAgentId().equals(excludeAgentId)) {
                continue;
            }
            callbacks.add(createCallback(agent));
        }
        log.info("Agent-as-Tool: 构建了 {} 个Agent工具 (排除agentId={})", callbacks.size(), excludeAgentId);
        return callbacks;
    }

    /**
     * 获取所有可作为工具的Agent信息列表（供REST API使用）。
     *
     * @return Agent信息列表, 包含agentId、名称、描述和工具名称
     */
    public List<AgentToolInfo> listAgentToolInfos() {
        List<AiAgent> allAgents = aiAgentMapper.selectAll();
        if (allAgents == null || allAgents.isEmpty()) {
            return Collections.emptyList();
        }
        List<AgentToolInfo> infos = new ArrayList<>();
        for (AiAgent agent : allAgents) {
            String toolName = "agent_" + agent.getAgentId().replaceAll("[^a-zA-Z0-9_]", "_");
            infos.add(new AgentToolInfo(
                    agent.getAgentId(),
                    agent.getAgentName(),
                    agent.getDescription(),
                    agent.getStrategy(),
                    toolName
            ));
        }
        return infos;
    }

    /**
     * 进入Agent调用栈, 用于递归保护。
     *
     * @param agentId 即将执行的agentId
     * @return true=允许执行, false=检测到递归调用应拒绝
     */
    public static boolean enterCallStack(String agentId) {
        return CALL_STACK.get().add(agentId);
    }

    /**
     * 离开Agent调用栈。
     *
     * @param agentId 执行完毕的agentId
     */
    public static void leaveCallStack(String agentId) {
        CALL_STACK.get().remove(agentId);
        // 如果调用栈为空, 清理ThreadLocal以防止内存泄漏
        if (CALL_STACK.get().isEmpty()) {
            CALL_STACK.remove();
        }
    }

    /**
     * 检查指定agentId是否已在当前调用栈中（即是否存在递归）。
     */
    public static boolean isInCallStack(String agentId) {
        return CALL_STACK.get().contains(agentId);
    }

    private AgentToolCallback createCallback(AiAgent agent) {
        return new AgentToolCallback(
                agent.getAgentId(),
                agent.getAgentName(),
                agent.getDescription(),
                agentService
        );
    }

    /**
     * Agent工具信息VO, 用于REST API返回。
     */
    public record AgentToolInfo(
            String agentId,
            String agentName,
            String description,
            String strategy,
            String toolName
    ) {
    }
}
