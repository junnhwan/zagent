package io.wanjune.zagent.config;

import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring AI 相关配置, 包含向量存储和文本分割器
 */
@Configuration
public class AiConfig {

    /**
     * 创建PgVectorStore Bean, 使用OpenAI Embedding模型, 向量表名: vector_store
     *
     * @param baseUrl      OpenAI API基础URL
     * @param apiKey       OpenAI API密钥
     * @param jdbcTemplate PgVector专用JdbcTemplate
     * @return PgVectorStore向量存储实例
     */
    @Bean("vectorStore")
    public PgVectorStore pgVectorStore(@Value("${spring.ai.openai.base-url}") String baseUrl,
                                        @Value("${spring.ai.openai.api-key}") String apiKey,
                                        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(openAiApi);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName("vector_store")
                .build();
    }

    /**
     * 创建文档文本分割器, 用于RAG文档切分
     *
     * @return TokenTextSplitter文本分割器实例
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

}
