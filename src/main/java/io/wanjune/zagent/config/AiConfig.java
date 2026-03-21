package io.wanjune.zagent.config;

import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.document.MetadataMode;
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
    public PgVectorStore pgVectorStore(@Value("${spring.ai.openai.embedding.base-url:${spring.ai.openai.base-url}}") String baseUrl,
                                        @Value("${spring.ai.openai.embedding.api-key:${spring.ai.openai.api-key}}") String apiKey,
                                        @Value("${spring.ai.openai.embedding.embeddings-path:/v1/embeddings}") String embeddingsPath,
                                        @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}") String embeddingModel,
                                        @Value("${spring.ai.openai.embedding.options.dimensions:0}") int embeddingDimensions,
                                        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .embeddingsPath(embeddingsPath)
                .build();

        OpenAiEmbeddingOptions.Builder optionsBuilder = OpenAiEmbeddingOptions.builder().model(embeddingModel);
        if (embeddingDimensions > 0) {
            optionsBuilder.dimensions(embeddingDimensions);
        }

        OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.NONE,
                optionsBuilder.build()
        );

        var storeBuilder = PgVectorStore.builder(jdbcTemplate, openAiEmbeddingModel)
                .vectorTableName("vector_store")
                .initializeSchema(true);
        if (embeddingDimensions > 0) {
            storeBuilder.dimensions(embeddingDimensions);
        }
        return storeBuilder.build();
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
