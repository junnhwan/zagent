package io.wanjune.zagent.chat.assembly.factory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.wanjune.zagent.rag.advisor.RagContextAdvisor;
import io.wanjune.zagent.model.entity.AiClientAdvisor;
import io.wanjune.zagent.model.entity.AiClientSystemPrompt;
import io.wanjune.zagent.model.enums.AdvisorTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiClientAdvisorFactory {

    @Resource
    @Qualifier("vectorStore")
    private VectorStore vectorStore;

    public String buildSystemPrompt(List<AiClientSystemPrompt> prompts) {
        return prompts.stream()
                .map(AiClientSystemPrompt::getPromptContent)
                .map(this::escapeTemplateBraces)
                .collect(Collectors.joining("\n\n"));
    }

    private String escapeTemplateBraces(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return prompt;
        }
        return prompt.replace("{", "\\{").replace("}", "\\}");
    }

    public List<Advisor> buildAdvisors(List<AiClientAdvisor> advisorConfigs) {
        List<Advisor> advisors = new ArrayList<>();
        for (AiClientAdvisor config : advisorConfigs) {
            try {
                Advisor advisor = createAdvisor(config);
                if (advisor != null) {
                    advisors.add(advisor);
                }
            } catch (Exception e) {
                log.error("Failed to create advisor: {} - {}", config.getAdvisorName(), e.getMessage());
            }
        }
        advisors.add(SimpleLoggerAdvisor.builder().build());
        return advisors;
    }

    private Advisor createAdvisor(AiClientAdvisor config) {
        AdvisorTypeEnum type = AdvisorTypeEnum.of(config.getAdvisorType());
        JSONObject extParam = config.getExtParam() != null ? JSON.parseObject(config.getExtParam()) : new JSONObject();

        return switch (type) {
            case CHAT_MEMORY -> {
                int maxMessages = extParam.getIntValue("maxMessages");
                if (maxMessages <= 0) {
                    maxMessages = 100;
                }
                yield PromptChatMemoryAdvisor.builder(
                        MessageWindowChatMemory.builder().maxMessages(maxMessages).build()
                ).build();
            }
            case RAG_ANSWER -> {
                int topK = extParam.getIntValue("topK");
                if (topK <= 0) {
                    topK = 5;
                }
                String filterExpression = extParam.getString("filterExpression");
                SearchRequest.Builder searchBuilder = SearchRequest.builder().topK(topK);
                if (filterExpression != null && !filterExpression.isBlank()) {
                    searchBuilder.filterExpression(filterExpression);
                }
                yield new RagContextAdvisor(vectorStore, searchBuilder.build());
            }
        };
    }
}
