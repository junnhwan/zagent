package io.wanjune.zagent.controller;

import io.wanjune.zagent.common.Result;
import io.wanjune.zagent.service.RagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG知识库控制器, 提供文档上传和智能查询接口
 *
 * @author zagent
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Resource
    private RagService ragService;

    /**
     * 上传文档到向量知识库, POST /api/rag/upload
     *
     * @param file         要上传的文档文件
     * @param knowledgeTag 知识标签, 用于向量检索时的过滤条件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam("knowledgeTag") String knowledgeTag) {
        try {
            ragService.uploadDocument(file, knowledgeTag);
            return Result.success("Document uploaded successfully");
        } catch (Exception e) {
            log.error("RAG upload error", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 基于RAG的智能查询, POST /api/rag/query
     *
     * @param clientId     AI客户端ID
     * @param question     用户问题
     * @param knowledgeTag 知识标签（可选）, 用于过滤检索范围
     * @return 基于知识库检索增强的AI回答
     */
    @PostMapping("/query")
    public Result<String> query(@RequestParam String clientId,
                                 @RequestParam String question,
                                 @RequestParam(required = false) String knowledgeTag) {
        try {
            String answer = ragService.query(clientId, question, knowledgeTag);
            return Result.success(answer);
        } catch (Exception e) {
            log.error("RAG query error", e);
            return Result.fail(e.getMessage());
        }
    }

}
