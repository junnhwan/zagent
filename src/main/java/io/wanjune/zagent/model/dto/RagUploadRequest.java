package io.wanjune.zagent.model.dto;

import lombok.Data;

/**
 * RAG文档上传请求DTO
 *
 * @author zagent
 */
@Data
public class RagUploadRequest {

    /** 知识标签, 用于向量检索时的过滤条件 */
    private String knowledgeTag;

}
