package io.wanjune.zagent.rag.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RAG上下文注入Advisor, 实现Spring AI的BaseAdvisor接口,
 * 在用户提问时自动从向量库检索相关文档并注入到prompt上下文中。
 * <p>修复了原始版本丢失系统提示词和历史消息的问题, 现在会保留原始对话上下文。</p>
 *
 * @author zagent
 */
@Slf4j
public class RagContextAdvisor implements BaseAdvisor {

    private static final String USER_TEXT_ADVISE = """

            Context information is below, surrounded by ---------------------

            ---------------------
            %s
            ---------------------

            Given the context and provided history information and not prior knowledge,
            reply to the user comment. If the answer is not in the context, inform
            the user that you can't answer the question.
            """;

    private static final String RETRIEVED_DOCUMENTS_KEY = "qa_retrieved_documents";
    private static final String FILTER_EXPRESSION_KEY = "qa_filter_expression";
    private static final int QUERY_PREVIEW_LIMIT = 80;
    private static final int DOCUMENT_PREVIEW_LIMIT = 80;

    private final VectorStore vectorStore;
    private final SearchRequest searchRequest;

    public RagContextAdvisor(VectorStore vectorStore, SearchRequest searchRequest) {
        this.vectorStore = vectorStore;
        this.searchRequest = searchRequest;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        Map<String, Object> context = new HashMap<>(request.context());

        // 1. 获取用户原始问题
        String userText = request.prompt().getUserMessage().getText();
        String filterExpressionText = resolveFilterExpressionText(context);

        // 2. 向量检索
        long startAt = System.currentTimeMillis();
        SearchRequest searchRequestToUse = SearchRequest.from(this.searchRequest)
                .query(userText)
                .filterExpression(resolveFilterExpression(context))
                .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequestToUse);
        long costMs = System.currentTimeMillis() - startAt;
        context.put(RETRIEVED_DOCUMENTS_KEY, documents);

        log.info("RAG检索完成: query='{}', filter='{}', hits={}, topK={}, threshold={}, costMs={}, docs=[{}]",
                abbreviate(userText, QUERY_PREVIEW_LIMIT),
                filterExpressionText,
                documents.size(),
                searchRequestToUse.getTopK(),
                searchRequestToUse.getSimilarityThreshold(),
                costMs,
                summarizeDocuments(documents));

        // 3. 拼接检索到的文档内容
        String documentContext = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

        // 4. 增强用户消息（注入RAG上下文）
        String advisedUserText = userText + System.lineSeparator()
                + String.format(USER_TEXT_ADVISE, documentContext);

        // 5. 保留原始消息（系统提示词、历史对话等），只替换用户消息
        List<Message> messages = new ArrayList<>();
        for (Message msg : request.prompt().getInstructions()) {
            if (msg instanceof UserMessage) {
                messages.add(new UserMessage(advisedUserText));
            } else {
                messages.add(msg);
            }
        }

        return ChatClientRequest.builder()
                .prompt(Prompt.builder().messages(messages).build())
                .context(context)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        ChatResponse.Builder builder = ChatResponse.builder().from(response.chatResponse());
        builder.metadata(RETRIEVED_DOCUMENTS_KEY, response.context().get(RETRIEVED_DOCUMENTS_KEY));
        return ChatClientResponse.builder()
                .chatResponse(builder.build())
                .context(response.context())
                .build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(this.before(request, chain));
        return this.after(response, chain);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        ChatClientRequest advisedRequest = this.before(request, chain);
        return chain.nextStream(advisedRequest)
                .map(resp -> this.after(resp, chain));
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    private Filter.Expression resolveFilterExpression(Map<String, Object> context) {
        if (context.containsKey(FILTER_EXPRESSION_KEY)
                && StringUtils.hasText(String.valueOf(context.get(FILTER_EXPRESSION_KEY)))) {
            return new FilterExpressionTextParser().parse(String.valueOf(context.get(FILTER_EXPRESSION_KEY)));
        }
        return this.searchRequest.getFilterExpression();
    }

    private String resolveFilterExpressionText(Map<String, Object> context) {
        if (context.containsKey(FILTER_EXPRESSION_KEY)
                && StringUtils.hasText(String.valueOf(context.get(FILTER_EXPRESSION_KEY)))) {
            return String.valueOf(context.get(FILTER_EXPRESSION_KEY));
        }
        return this.searchRequest.hasFilterExpression() ? String.valueOf(this.searchRequest.getFilterExpression()) : "<none>";
    }

    public static String summarizeDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "hits=0";
        }
        return IntStream.range(0, documents.size())
                .mapToObj(index -> {
                    Document document = documents.get(index);
                    String source = metadataValue(document, "source");
                    String knowledge = metadataValue(document, "knowledge");
                    return "#" + (index + 1) + "{source=" + source
                            + ", knowledge=" + knowledge
                            + ", preview=" + abbreviate(document.getText(), DOCUMENT_PREVIEW_LIMIT)
                            + "}";
                })
                .collect(Collectors.joining(", "));
    }

    private static String metadataValue(Document document, String key) {
        Object value = document.getMetadata().get(key);
        return value == null ? "<none>" : String.valueOf(value);
    }

    private static String abbreviate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "<empty>";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

}
