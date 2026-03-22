package io.wanjune.zagent.service.impl;

import io.wanjune.zagent.model.entity.AiClientApi;
import io.wanjune.zagent.model.entity.AiClientModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiClientModelFactory {

    public OpenAiApi createOpenAiApi(AiClientApi apiConfig) {
        return OpenAiApi.builder()
                .baseUrl(apiConfig.getBaseUrl())
                .apiKey(apiConfig.getApiKey())
                .completionsPath(apiConfig.getCompletionsPath())
                .embeddingsPath(apiConfig.getEmbeddingsPath())
                .build();
    }

    public OpenAiChatModel createChatModel(AiClientModel modelConfig, OpenAiApi openAiApi, List<ToolCallback> toolCallbacks) {
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(modelConfig.getModelName());
        if (toolCallbacks != null && !toolCallbacks.isEmpty()) {
            optionsBuilder.toolCallbacks(toolCallbacks);
        }

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(optionsBuilder.build())
                .build();
    }
}
