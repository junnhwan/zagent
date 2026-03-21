package io.wanjune.zagent.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import io.wanjune.zagent.advisor.RagContextAdvisor;
import io.wanjune.zagent.service.AiClientAssemblyService;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagServiceImplTest {

    @Test
    void listKnowledgeTagsThrowsHelpfulMessageWhenPgVectorIsUnavailable() {
        RagServiceImpl service = new RagServiceImpl();
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        when(jdbcTemplate.queryForList(anyString())).thenThrow(
                new CannotGetJdbcConnectionException("connect failed", new SQLException("Connection refused"))
        );
        ReflectionTestUtils.setField(service, "pgJdbcTemplate", jdbcTemplate);

        assertThatThrownBy(service::listKnowledgeTags)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PgVector")
                .hasMessageContaining("spring.datasource.pgvector");
    }

    @Test
    void uploadDocumentThrowsHelpfulMessageWhenEmbeddingEndpointReturns404() {
        RagServiceImpl service = new RagServiceImpl();
        VectorStore vectorStore = mock(VectorStore.class);
        TokenTextSplitter tokenTextSplitter = mock(TokenTextSplitter.class);

        when(tokenTextSplitter.apply(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new Document("hello world")));
        doThrow(new NonTransientAiException("404 - 404 page not found"))
                .when(vectorStore)
                .accept(anyList());

        ReflectionTestUtils.setField(service, "vectorStore", vectorStore);
        ReflectionTestUtils.setField(service, "tokenTextSplitter", tokenTextSplitter);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.md",
                "text/markdown",
                "hello world".getBytes()
        );

        assertThatThrownBy(() -> service.uploadDocument(file, "测试标签"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Embedding")
                .hasMessageContaining("404");
    }

    @Test
    void queryAlwaysAddsRagAdvisorForRagEndpoint() {
        RagServiceImpl service = new RagServiceImpl();
        AiClientAssemblyService assemblyService = mock(AiClientAssemblyService.class);
        VectorStore vectorStore = mock(VectorStore.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(assemblyService.getOrBuildChatClient("3001")).thenReturn(chatClient);
        when(chatClient.prompt("你看得到我上传的文档吗")).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Advisor[].class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.system(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("answer");

        ReflectionTestUtils.setField(service, "aiClientAssemblyService", assemblyService);
        ReflectionTestUtils.setField(service, "vectorStore", vectorStore);
        ReflectionTestUtils.setField(service, "ragTopK", 5);
        ReflectionTestUtils.setField(service, "ragSimilarityThreshold", 0.0d);

        String answer = service.query("3001", "你看得到我上传的文档吗", "客服文档");

        assertThat(answer).isEqualTo("answer");

        ArgumentCaptor<Advisor[]> advisorCaptor = ArgumentCaptor.forClass(Advisor[].class);
        verify(requestSpec).advisors(advisorCaptor.capture());
        assertThat(advisorCaptor.getValue()).hasSize(1);
        assertThat(advisorCaptor.getValue()[0]).isInstanceOf(RagContextAdvisor.class);
    }
}
