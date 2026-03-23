package io.wanjune.zagent.rag.service;

import io.wanjune.zagent.chat.assembly.AiClientAssemblyService;
import io.wanjune.zagent.rag.advisor.RagContextAdvisor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG服务实现。
 * <p>文档处理流程: Tika读取 -> TokenTextSplitter分块 -> 添加元数据 -> PgVector存储。
 * 查询时通过AiClientAssemblyService获取ChatClient, 结合知识库标签过滤进行RAG增强查询。</p>
 *
 * @author zagent
 */
@Slf4j
@Service
public class RagServiceImpl implements RagService {

    @Resource
    @Qualifier("vectorStore")
    private VectorStore vectorStore;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private AiClientAssemblyService aiClientAssemblyService;

    @Resource
    @Qualifier("pgVectorJdbcTemplate")
    private JdbcTemplate pgJdbcTemplate;

    @Value("${zagent.rag.top-k:5}")
    private int ragTopK;

    @Value("${zagent.rag.similarity-threshold:0.0}")
    private double ragSimilarityThreshold;

    @Override
    public void uploadDocument(MultipartFile file, String knowledgeTag) {
        log.info("上传文档: {}, 知识标签: {}", file.getOriginalFilename(), knowledgeTag);

        try {
            TikaDocumentReader reader = new TikaDocumentReader(new InputStreamResource(file.getInputStream()));
            List<Document> documents = reader.get();

            List<Document> splitDocs = tokenTextSplitter.apply(documents);

            for (Document doc : splitDocs) {
                doc.getMetadata().putAll(Map.of(
                        "knowledge", knowledgeTag,
                        "source", file.getOriginalFilename()
                ));
            }

            log.info("文档切块完成: source={}, knowledgeTag={}, {}",
                    file.getOriginalFilename(),
                    knowledgeTag,
                    summarizeSplitDocuments(splitDocs));

            vectorStore.accept(splitDocs);
            log.info("文档上传成功, 共{}个分块", splitDocs.size());
        } catch (Exception e) {
            throw translateUploadException(e);
        }
    }

    @Override
    public String query(String clientId, String question, String knowledgeTag) {
        ChatClient chatClient = aiClientAssemblyService.getOrBuildChatClient(clientId);

        return chatClient.prompt(question)
                .advisors(buildRagAdvisor())
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .advisors(a -> {
                    if (knowledgeTag != null && !knowledgeTag.isBlank()) {
                        a.param("qa_filter_expression", "knowledge == '" + knowledgeTag + "'");
                    }
                })
                .call()
                .content();
    }

    private RagContextAdvisor buildRagAdvisor() {
        SearchRequest.Builder searchBuilder = SearchRequest.builder().topK(ragTopK);
        if (ragSimilarityThreshold > 0) {
            searchBuilder.similarityThreshold(ragSimilarityThreshold);
        }
        return new RagContextAdvisor(vectorStore, searchBuilder.build());
    }

    static String summarizeSplitDocuments(List<Document> splitDocs) {
        if (splitDocs == null || splitDocs.isEmpty()) {
            return "chunks=0";
        }
        List<String> details = new ArrayList<>();
        for (int index = 0; index < splitDocs.size(); index++) {
            Document document = splitDocs.get(index);
            details.add("#" + (index + 1)
                    + "{length=" + document.getText().length()
                    + ", preview=" + abbreviate(document.getText(), 80)
                    + "}");
        }
        return "chunks=" + splitDocs.size() + ", details=[" + details.stream().collect(Collectors.joining(", ")) + "]";
    }

    private static String abbreviate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "<empty>";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    @Override
    public int deleteByKnowledgeTag(String knowledgeTag) {
        log.info("删除知识标签: {}", knowledgeTag);
        try {
            int count = pgJdbcTemplate.update(
                    "DELETE FROM vector_store WHERE metadata->>'knowledge' = ?",
                    knowledgeTag);
            log.info("已删除{}条文档记录", count);
            return count;
        } catch (DataAccessException e) {
            throw translateVectorStoreConnectionException("删除知识标签", e);
        }
    }

    @Override
    public List<Map<String, Object>> listKnowledgeTags() {
        try {
            return pgJdbcTemplate.queryForList(
                    "SELECT metadata->>'knowledge' AS tag, COUNT(*) AS doc_count " +
                    "FROM vector_store " +
                    "WHERE metadata->>'knowledge' IS NOT NULL " +
                    "GROUP BY metadata->>'knowledge' " +
                    "ORDER BY doc_count DESC");
        } catch (DataAccessException e) {
            throw translateVectorStoreConnectionException("查询知识标签", e);
        }
    }

    private RuntimeException translateUploadException(Exception exception) {
        // 提取根因, 便于精确诊断
        Throwable rootCause = exception;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        String rootMsg = rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().getSimpleName();

        if (exception instanceof NonTransientAiException) {
            return new RuntimeException(
                    "上传文档失败: Embedding API 调用异常，请检查 embedding 的 base-url、api-key 和 model 配置。错误: " + rootMsg,
                    exception);
        }

        if (exception instanceof CannotGetJdbcConnectionException) {
            return translateVectorStoreConnectionException("上传文档", exception);
        }

        if (exception instanceof DataAccessException) {
            // DataAccessException 可能由 Embedding 调用失败间接触发, 输出根因帮助定位
            return new RuntimeException("上传文档失败: " + rootMsg, exception);
        }

        return new RuntimeException("上传文档失败: " + rootMsg, exception);
    }

    private RuntimeException translateVectorStoreConnectionException(String action, Exception exception) {
        return new RuntimeException(
                action + "失败: PgVector 向量库连接不可用，请检查 spring.datasource.pgvector.* 配置以及 PostgreSQL 服务是否可达。",
                exception
        );
    }

}
