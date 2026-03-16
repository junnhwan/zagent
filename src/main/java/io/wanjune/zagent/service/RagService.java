package io.wanjune.zagent.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * RAG(检索增强生成)服务接口, 支持文档上传和基于知识库的智能查询。
 */
public interface RagService {

    /**
     * 上传文档到PgVector向量库, 自动分块+生成Embedding。
     * <p>文档经Tika解析后, 通过TokenTextSplitter分块, 添加知识标签元数据,
     * 最终存储到PgVector向量数据库中。</p>
     *
     * @param file         待上传的文档文件（支持Tika可解析的格式: PDF、Word、TXT等）
     * @param knowledgeTag 知识库标签, 用于后续按标签过滤检索
     */
    void uploadDocument(MultipartFile file, String knowledgeTag);

    /**
     * 使用RAG上下文执行智能查询。
     * <p>根据用户问题从向量库中检索相关文档片段, 注入到AI上下文中进行增强回答。</p>
     *
     * @param clientId     AI客户端标识ID
     * @param question     用户问题
     * @param knowledgeTag 知识库标签, 用于过滤检索范围（可为null表示不过滤）
     * @return AI基于RAG上下文生成的回答内容
     */
    String query(String clientId, String question, String knowledgeTag);

}
