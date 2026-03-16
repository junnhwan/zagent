package io.wanjune.zagent.service.impl;

import io.wanjune.zagent.service.AiClientAssemblyService;
import io.wanjune.zagent.service.RagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * RAG服务实现。
 * <p>文档处理流程: Tika读取 -> TokenTextSplitter分块 -> 添加元数据 -> PgVector存储。
 * 查询时通过AiClientAssemblyService获取ChatClient, 结合知识库标签过滤进行RAG增强查询。</p>
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

    /** {@inheritDoc} */
    @Override
    public void uploadDocument(MultipartFile file, String knowledgeTag) {
        try {
            log.info("Uploading document: {}, knowledgeTag: {}", file.getOriginalFilename(), knowledgeTag);

            // Read document using Tika
            TikaDocumentReader reader = new TikaDocumentReader(new InputStreamResource(file.getInputStream()));
            List<Document> documents = reader.get();

            // Split into chunks
            List<Document> splitDocs = tokenTextSplitter.apply(documents);

            // Add knowledge tag metadata
            for (Document doc : splitDocs) {
                doc.getMetadata().putAll(Map.of(
                        "knowledge", knowledgeTag,
                        "source", file.getOriginalFilename()
                ));
            }

            // Store in vector database
            vectorStore.accept(splitDocs);

            log.info("Document uploaded successfully: {} chunks stored", splitDocs.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String query(String clientId, String question, String knowledgeTag) {
        ChatClient chatClient = aiClientAssemblyService.getOrBuildChatClient(clientId);

        return chatClient.prompt(question)
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .advisors(a -> {
                    if (knowledgeTag != null && !knowledgeTag.isBlank()) {
                        a.param("qa_filter_expression", "knowledge == '" + knowledgeTag + "'");
                    }
                })
                .call()
                .content();
    }

}
