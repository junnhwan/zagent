package io.wanjune.zagent.agent.strategy;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * Agent执行策略接口, 所有编排策略（Fixed/Auto/Flow）均实现此接口
 *
 * @author zagent
 */
public interface IExecuteStrategy {

    /**
     * 执行Agent策略
     *
     * @param context 执行上下文
     * @param emitter SSE发射器, 用于流式推送执行过程; 为null表示同步模式
     * @return 最终输出文本
     */
    String execute(ExecuteContext context, SseEmitter emitter) throws Exception;

    /**
     * 执行上下文, 封装策略执行所需的所有信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ExecuteContext {
        /** 智能体ID */
        private String agentId;
        /** 用户原始输入 */
        private String userInput;
        /** 对话ID */
        private String conversationId;
        /** 最大执行轮次(Auto策略用) */
        @Builder.Default
        private int maxStep = 3;
        /** 客户端类型映射: clientType -> clientId */
        private Map<String, String> clientTypeMap;
        /** 步骤提示词映射: clientType -> stepPrompt */
        private Map<String, String> stepPromptMap;
    }

    /**
     * SSE阶段事件, 用于流式推送执行进度
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class StageEvent {
        /** 阶段名称: analysis/execution/supervision/summary/plan/step_execution/tool_analysis/complete */
        private String stage;
        /** 事件状态: active(进行中)/done(完成)/error(失败) */
        @Builder.Default
        private String status = "done";
        /** 当前轮次 */
        private int step;
        /** 总步骤数(用于进度显示) */
        private int totalSteps;
        /** 阶段内容 */
        private String content;
        /** 结构化扩展载荷（如规划步骤列表） */
        private Object payload;
        /** 会话ID */
        private String sessionId;
    }

}
