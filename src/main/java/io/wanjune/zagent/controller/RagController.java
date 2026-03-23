package io.wanjune.zagent.controller;

import io.wanjune.zagent.common.Result;
import io.wanjune.zagent.rag.service.RagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * RAG知识库控制器, 提供文档上传、删除、查询和标签管理接口
 *
 * @author zagent
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class RagController {

    @Resource
    private RagService ragService;

    /**
     * 上传文档到向量知识库, POST /api/rag/upload
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam("knowledgeTag") String knowledgeTag) {
        ragService.uploadDocument(file, knowledgeTag);
        return Result.success("文档上传成功");
    }

    /**
     * 基于RAG的智能查询, POST /api/rag/query
     */
    @PostMapping("/query")
    public Result<String> query(@RequestParam String clientId,
                                 @RequestParam String question,
                                 @RequestParam(required = false) String knowledgeTag) {
        String answer = ragService.query(clientId, question, knowledgeTag);
        return Result.success(answer);
    }

    /**
     * 删除指定知识标签的所有文档, DELETE /api/rag/{knowledgeTag}
     */
    @DeleteMapping("/{knowledgeTag}")
    public Result<Integer> delete(@PathVariable String knowledgeTag) {
        int count = ragService.deleteByKnowledgeTag(knowledgeTag);
        return Result.success(count);
    }

    /**
     * 查询所有知识标签及文档数量, GET /api/rag/tags
     */
    @GetMapping("/tags")
    public Result<List<Map<String, Object>>> listTags() {
        return Result.success(ragService.listKnowledgeTags());
    }

}
