package io.wanjune.zagent.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * RAG(检索增强生成)服务接口, 支持文档上传、删除和基于知识库的智能查询。
 */
public interface RagService {

    /**
     * 上传文档到PgVector向量库, 自动分块+生成Embedding
     *
     * @param file         待上传的文档文件
     * @param knowledgeTag 知识库标签
     */
    void uploadDocument(MultipartFile file, String knowledgeTag);

    /**
     * 使用RAG上下文执行智能查询
     *
     * @param clientId     AI客户端标识ID
     * @param question     用户问题
     * @param knowledgeTag 知识库标签（可为null）
     * @return AI基于RAG上下文生成的回答
     */
    String query(String clientId, String question, String knowledgeTag);

    /**
     * 删除指定知识标签的所有文档
     *
     * @param knowledgeTag 知识库标签
     * @return 删除的文档数量
     */
    int deleteByKnowledgeTag(String knowledgeTag);

    /**
     * 查询所有已上传的知识标签列表
     *
     * @return 知识标签及其文档数量
     */
    List<Map<String, Object>> listKnowledgeTags();

}
