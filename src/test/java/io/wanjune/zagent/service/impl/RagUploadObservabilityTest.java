package io.wanjune.zagent.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RagUploadObservabilityTest {

    @Test
    void summarizeSplitDocumentsIncludesChunkLengthsAndPreview() {
        List<Document> documents = List.of(
                new Document("第一段退款规则内容", Map.of("source", "test_rag.txt", "knowledge", "客服文档")),
                new Document("第二段物流规则内容", Map.of("source", "test_rag.txt", "knowledge", "客服文档"))
        );

        String summary = RagServiceImpl.summarizeSplitDocuments(documents);

        assertThat(summary).contains("chunks=2");
        assertThat(summary).contains("#1{");
        assertThat(summary).contains("length=9");
        assertThat(summary).contains("preview=第一段退款规则内容");
        assertThat(summary).contains("#2{");
    }
}
