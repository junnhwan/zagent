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
 * Spring AI 相关配置, 包含向量存储和文本分割器。
 * <p>分块参数支持通过application.yml配置:
 * <ul>
 *   <li>zagent.rag.chunk-size: 分块大小（默认800 tokens）</li>
 *   <li>zagent.rag.chunk-overlap: 分块重叠（默认100 tokens）</li>
 *   <li>zagent.rag.similarity-threshold: 相似度阈值（默认0.0）</li>
 *   <li>zagent.rag.top-k: 检索TopK（默认5）</li>
 * </ul></p>
 */
@Configuration
public class AiConfig {

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
     * 可配置的文本分割器, 支持自定义分块大小和重叠
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter(
            @Value("${zagent.rag.chunk-size:800}") int chunkSize,
            @Value("${zagent.rag.chunk-overlap:100}") int chunkOverlap) {
        return new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
    }

}
