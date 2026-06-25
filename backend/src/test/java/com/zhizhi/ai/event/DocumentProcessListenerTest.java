package com.zhizhi.ai.event;

import com.zhizhi.ai.service.DocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentProcessListener 单元测试")
class DocumentProcessListenerTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentProcessListener listener;

    @Test
    @DisplayName("onDocumentProcess 委托给 DocumentService.processDocumentAsync")
    void onDocumentProcess_delegatesToService() {
        byte[] content = {1, 2, 3};
        DocumentProcessEvent event = new DocumentProcessEvent(
                1L, content, "test.pdf", 10L);

        listener.onDocumentProcess(event);

        verify(documentService).processDocumentAsync(1L, content, "test.pdf", 10L);
    }

    @Test
    @DisplayName("onDocumentProcess contentBytes 为 null 时仍然委托")
    void onDocumentProcess_nullContentBytes() {
        DocumentProcessEvent event = new DocumentProcessEvent(
                2L, null, "empty.pdf", 20L);

        listener.onDocumentProcess(event);

        verify(documentService).processDocumentAsync(2L, null, "empty.pdf", 20L);
    }

    @Test
    @DisplayName("onDocumentProcess 使用正确的参数调用一次")
    void onDocumentProcess_calledOnce() {
        DocumentProcessEvent event = new DocumentProcessEvent(
                5L, new byte[]{10}, "doc.md", 30L);

        listener.onDocumentProcess(event);

        verify(documentService, times(1))
                .processDocumentAsync(5L, new byte[]{10}, "doc.md", 30L);
    }
}
