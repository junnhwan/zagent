package io.wanjune.zagent.agent.strategy.impl;

import io.wanjune.zagent.agent.strategy.IExecuteStrategy;
import io.wanjune.zagent.chat.assembly.AiClientAssemblyService;
import io.wanjune.zagent.chat.assembly.model.AssembledAiClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReActExecuteStrategyExecuteTest {

    @Test
    void executeShouldCallToolAndReturnFinalAnswer() throws Exception {
        ReActExecuteStrategy strategy = new ReActExecuteStrategy();

        AiClientAssemblyService assemblyService = Mockito.mock(AiClientAssemblyService.class);
        Field field = ReActExecuteStrategy.class.getDeclaredField("aiClientAssemblyService");
        field.setAccessible(true);
        field.set(strategy, assemblyService);

        OpenAiChatModel chatModel = Mockito.mock(OpenAiChatModel.class);
        ToolCallback toolCallback = Mockito.mock(ToolCallback.class);
        ToolDefinition toolDefinition = Mockito.mock(ToolDefinition.class);
        when(toolCallback.getToolDefinition()).thenReturn(toolDefinition);
        when(toolDefinition.name()).thenReturn("weather_query");
        when(toolCallback.call("{\"city\":\"上海\"}")).thenReturn("晴，26度");

        AssembledAiClient assembledAiClient = new AssembledAiClient(
                null,
                chatModel,
                "你是一个会使用工具的助手。",
                List.of(),
                List.of(toolCallback)
        );
        when(assemblyService.getOrBuildAssembledClient("react-client")).thenReturn(assembledAiClient);

        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                "call_1",
                "function",
                "weather_query",
                "{\"city\":\"上海\"}"
        );
        AssistantMessage firstAssistant = new AssistantMessage(
                "我先查询天气。",
                Map.of(),
                List.of(toolCall),
                List.of()
        );
        AssistantMessage finalAssistant = new AssistantMessage("上海今天天气晴，26度。", Map.of());

        ChatResponse firstResponse = mockChatResponse(firstAssistant);
        ChatResponse finalResponse = mockChatResponse(finalAssistant);
        when(chatModel.call(any(Prompt.class))).thenReturn(firstResponse, finalResponse);

        IExecuteStrategy.ExecuteContext context = IExecuteStrategy.ExecuteContext.builder()
                .agentId("agent-1")
                .userInput("上海今天天气怎么样？")
                .conversationId("conv-1")
                .maxStep(3)
                .clientTypeMap(Map.of("reactor", "react-client"))
                .stepPromptMap(Map.of())
                .build();

        String result = strategy.execute(context, null);

        assertThat(result).isEqualTo("上海今天天气晴，26度。");
        verify(toolCallback, times(1)).call("{\"city\":\"上海\"}");
        verify(chatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void executeShouldUseFallbackToolClientWhenReasoningClientHasNoTools() throws Exception {
        ReActExecuteStrategy strategy = new ReActExecuteStrategy();

        AiClientAssemblyService assemblyService = Mockito.mock(AiClientAssemblyService.class);
        Field field = ReActExecuteStrategy.class.getDeclaredField("aiClientAssemblyService");
        field.setAccessible(true);
        field.set(strategy, assemblyService);

        OpenAiChatModel reasoningModel = Mockito.mock(OpenAiChatModel.class);
        ToolCallback toolCallback = Mockito.mock(ToolCallback.class);
        ToolDefinition toolDefinition = Mockito.mock(ToolDefinition.class);
        when(toolCallback.getToolDefinition()).thenReturn(toolDefinition);
        when(toolDefinition.name()).thenReturn("weather_query");
        when(toolCallback.call("{\"city\":\"北京\"}")).thenReturn("多云，22度");

        when(assemblyService.getOrBuildAssembledClient("react-client")).thenReturn(new AssembledAiClient(
                null, reasoningModel, "system", List.of(), List.of()
        ));
        when(assemblyService.getOrBuildAssembledClient("tool-client")).thenReturn(new AssembledAiClient(
                null, Mockito.mock(OpenAiChatModel.class), "tool-system", List.of(), List.of(toolCallback)
        ));

        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                "call_2",
                "function",
                "weather_query",
                "{\"city\":\"北京\"}"
        );
        ChatResponse firstReasoningResponse = mockChatResponse(
                new AssistantMessage("查询中", Map.of(), List.of(toolCall), List.of())
        );
        ChatResponse secondReasoningResponse = mockChatResponse(
                new AssistantMessage("北京多云，22度。", Map.of())
        );
        when(reasoningModel.call(any(Prompt.class))).thenReturn(
                firstReasoningResponse,
                secondReasoningResponse
        );

        IExecuteStrategy.ExecuteContext context = IExecuteStrategy.ExecuteContext.builder()
                .agentId("agent-2")
                .userInput("北京天气")
                .conversationId("conv-2")
                .maxStep(2)
                .clientTypeMap(Map.of("reactor", "react-client", "tool_mcp", "tool-client"))
                .stepPromptMap(Map.of())
                .build();

        String result = strategy.execute(context, null);

        assertThat(result).isEqualTo("北京多云，22度。");
        verify(assemblyService).getOrBuildAssembledClient("react-client");
        verify(assemblyService).getOrBuildAssembledClient("tool-client");
        verify(toolCallback).call("{\"city\":\"北京\"}");
    }

    private ChatResponse mockChatResponse(AssistantMessage assistantMessage) {
        Generation generation = Mockito.mock(Generation.class);
        when(generation.getOutput()).thenReturn(assistantMessage);

        ChatResponse response = Mockito.mock(ChatResponse.class);
        when(response.getResult()).thenReturn(generation);
        return response;
    }
}
