package io.wanjune.zagent.advisor;

import io.wanjune.zagent.rag.advisor.RagContextAdvisor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RagContextAdvisorTest {

    @Test
    void summarizeDocumentsIncludesSourceKnowledgeAndPreview() {
        List<Document> documents = List.of(
                new Document("支付成功后不支持立即退款，需在订单发货前申请。", Map.of("source", "test_rag.txt", "knowledge", "客服文档")),
                new Document("退款审核周期为1~3个工作日。", Map.of("source", "test_rag.txt", "knowledge", "客服文档"))
        );

        String summary = RagContextAdvisor.summarizeDocuments(documents);

        assertThat(summary).contains("#1{");
        assertThat(summary).contains("source=test_rag.txt");
        assertThat(summary).contains("knowledge=客服文档");
        assertThat(summary).contains("preview=支付成功后不支持立即退款");
        assertThat(summary).contains("#2{");
    }

    @Test
    void summarizeDocumentsReturnsZeroHitsForEmptyList() {
        assertThat(RagContextAdvisor.summarizeDocuments(List.of())).isEqualTo("hits=0");
    }
}
