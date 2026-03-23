package io.wanjune.zagent.chat.assembly.model;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

public record AssembledAiClient(
        ChatClient chatClient,
        OpenAiChatModel chatModel,
        String systemPrompt,
        List<Advisor> advisors,
        List<ToolCallback> toolCallbacks
) {
}
