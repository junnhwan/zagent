package io.wanjune.zagent.advisor;

import com.alibaba.fastjson.JSON;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG上下文注入Advisor, 实现Spring AI的BaseAdvisor接口,
 * 在用户提问时自动从向量库检索相关文档并注入到prompt上下文中
 *
 * @author zagent
 */
public class RagContextAdvisor implements BaseAdvisor {

    /** RAG上下文注入的提示词模板, 指导AI基于检索到的上下文回答问题 */
    private static final String USER_TEXT_ADVISE = """

            Context information is below, surrounded by ---------------------

            ---------------------
            {question_answer_context}
            ---------------------

            Given the context and provided history information and not prior knowledge,
            reply to the user comment. If the answer is not in the context, inform
            the user that you can't answer the question.
            """;

    /** 上下文中存储检索到文档的key */
    private static final String RETRIEVED_DOCUMENTS_KEY = "qa_retrieved_documents";
    /** 上下文中存储过滤表达式的key */
    private static final String FILTER_EXPRESSION_KEY = "qa_filter_expression";

    /** 向量存储, 用于相似度检索 */
    private final VectorStore vectorStore;
    /** 检索配置（topK, filterExpression等） */
    private final SearchRequest searchRequest;

    /**
     * 构造函数
     *
     * @param vectorStore   向量存储
     * @param searchRequest 检索配置(topK, filterExpression等)
     */
    public RagContextAdvisor(VectorStore vectorStore, SearchRequest searchRequest) {
        this.vectorStore = vectorStore;
        this.searchRequest = searchRequest;
    }

    /**
     * 请求前置处理 - 执行向量相似度搜索, 将检索到的文档内容注入用户消息
     *
     * @param request      原始请求
     * @param advisorChain advisor链
     * @return 注入了RAG上下文的新请求
     */
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        Map<String, Object> context = new HashMap<>(request.context());

        String userText = request.prompt().getUserMessage().getText();
        String advisedUserText = userText + System.lineSeparator() + USER_TEXT_ADVISE;

        String query = new PromptTemplate(userText).render();
        SearchRequest searchRequestToUse = SearchRequest.from(this.searchRequest)
                .query(query)
                .filterExpression(resolveFilterExpression(context))
                .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequestToUse);
        context.put(RETRIEVED_DOCUMENTS_KEY, documents);

        String documentContext = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

        Map<String, Object> advisedParams = new HashMap<>(request.context());
        advisedParams.put("question_answer_context", documentContext);

        return ChatClientRequest.builder()
                .prompt(Prompt.builder()
                        .messages(new UserMessage(advisedUserText), new AssistantMessage(JSON.toJSONString(advisedParams)))
                        .build())
                .context(advisedParams)
                .build();
    }

    /**
     * 响应后置处理 - 将检索到的文档附加到响应元数据
     *
     * @param response     原始响应
     * @param advisorChain advisor链
     * @return 附加了检索文档元数据的新响应
     */
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
        return BaseAdvisor.super.adviseStream(request, chain);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 解析过滤表达式, 优先使用上下文中的动态表达式
     *
     * @param context 请求上下文
     * @return 过滤表达式, 若上下文中无动态表达式则使用默认配置
     */
    private Filter.Expression resolveFilterExpression(Map<String, Object> context) {
        if (context.containsKey(FILTER_EXPRESSION_KEY)
                && StringUtils.hasText(String.valueOf(context.get(FILTER_EXPRESSION_KEY)))) {
            return new FilterExpressionTextParser().parse(String.valueOf(context.get(FILTER_EXPRESSION_KEY)));
        }
        return this.searchRequest.getFilterExpression();
    }

}
