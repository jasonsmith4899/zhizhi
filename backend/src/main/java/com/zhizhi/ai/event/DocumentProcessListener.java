package com.zhizhi.ai.event;

import com.zhizhi.ai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 文档处理事件监听器。
 * 在事务 AFTER_COMMIT 阶段触发，确保文档记录已落库；
 * 实际处理委托给 @Async 的 processDocumentAsync，不阻塞调用方。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessListener {

    private final DocumentService documentService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentProcess(DocumentProcessEvent event) {
        log.info("收到文档处理事件: docId={}, kbId={}", event.documentId(), event.knowledgeBaseId());
        documentService.processDocumentAsync(
                event.documentId(), event.contentBytes(), event.filename(), event.knowledgeBaseId());
    }
}
