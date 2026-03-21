package io.wanjune.zagent.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AiConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiConfig.class, TestConfig.class)
            .withPropertyValues(
                    "spring.ai.openai.base-url=https://chat.example.com",
                    "spring.ai.openai.api-key=chat-key",
                    "spring.ai.openai.embedding.base-url=https://embedding.example.com",
                    "spring.ai.openai.embedding.api-key=embedding-key",
                    "spring.ai.openai.embedding.options.model=text-embedding-v2"
            );

    @Test
    void vectorStoreUsesEmbeddingSpecificOpenAiConfiguration() throws Exception {
        contextRunner.run(context -> {
            PgVectorStore vectorStore = context.getBean("vectorStore", PgVectorStore.class);

            Object embeddingModel = ReflectionTestUtils.getField(vectorStore, "embeddingModel");
            Object openAiApi = ReflectionTestUtils.getField(embeddingModel, "openAiApi");
            Object defaultOptions = ReflectionTestUtils.getField(embeddingModel, "defaultOptions");

            String baseUrl = invokeString(openAiApi, "getBaseUrl");
            ApiKey apiKey = (ApiKey) invoke(openAiApi, "getApiKey");
            String model = (String) invoke(defaultOptions, "getModel");

            assertThat(baseUrl).isEqualTo("https://embedding.example.com");
            assertThat(apiKey.getValue()).isEqualTo("embedding-key");
            assertThat(model).isEqualTo("text-embedding-v2");
        });
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static String invokeString(Object target, String methodName) throws Exception {
        return (String) invoke(target, methodName);
    }

    @Configuration
    static class TestConfig {

        @Bean("pgVectorJdbcTemplate")
        JdbcTemplate pgVectorJdbcTemplate() {
            return Mockito.mock(JdbcTemplate.class);
        }
    }
}
