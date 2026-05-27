package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.Result;
import com.zhizhi.ai.model.dto.DocumentListDTO;
import com.zhizhi.ai.model.entity.Document;
import com.zhizhi.ai.model.entity.DocumentChunk;
import com.zhizhi.ai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final AuthUtil authUtil;

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            Authentication authentication) throws IOException {

        Long userId = authUtil.getUserId(authentication);
        Document doc = documentService.uploadDocument(knowledgeBaseId, file, userId);

        return Result.ok(Map.of(
                "id", doc.getId(),
                "filename", doc.getFilename(),
                "status", doc.getStatus(),
                "message", "文档已上传，正在处理中..."
        ));
    }

    @GetMapping
    public Result<List<DocumentListDTO>> list(
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        List<DocumentListDTO> dtos = documentService.listByKnowledgeBase(knowledgeBaseId, userId)
                .stream()
                .map(DocumentListDTO::fromEntity)
                .toList();
        return Result.ok(dtos);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        documentService.deleteDocument(id, userId);
        return Result.ok();
    }

    @GetMapping("/{id}/chunks")
    public Result<List<DocumentChunk>> getChunks(@PathVariable Long id, Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        return Result.ok(documentService.getDocumentChunks(id, userId));
    }

    @GetMapping("/{id}/vector-status")
    public Result<Map<String, Object>> getVectorStatus(@PathVariable Long id, Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        return Result.ok(documentService.getVectorStatus(id, userId));
    }

    @PostMapping("/{id}/re-vectorize")
    public Result<Map<String, String>> reVectorize(@PathVariable Long id, Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        documentService.reVectorize(id, userId);
        return Result.ok(Map.of("message", "重新向量化已启动"));
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> body, Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        List<Long> ids = body.get("ids");
        documentService.batchDelete(ids, userId);
        return Result.ok();
    }

    @GetMapping("/{id}/preview")
    public Result<Map<String, Object>> preview(@PathVariable Long id, Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        return Result.ok(documentService.getDocumentPreview(id, userId));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id, Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        Map<String, Object> result = documentService.getDocumentDownload(id, userId);

        String filename = (String) result.get("filename");
        byte[] content = (byte[]) result.get("content");
        String contentType = (String) result.get("contentType");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }
}
