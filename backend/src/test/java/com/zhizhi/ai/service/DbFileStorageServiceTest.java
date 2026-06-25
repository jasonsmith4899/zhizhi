package com.zhizhi.ai.service;

import com.zhizhi.ai.model.entity.DocumentFile;
import com.zhizhi.ai.repository.DocumentFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DbFileStorageServiceTest {

    @Mock
    private DocumentFileRepository fileRepository;

    @InjectMocks
    private DbFileStorageService dbFileStorageService;

    // ---------- store ----------

    @Test
    void store_success() {
        when(fileRepository.save(any(DocumentFile.class))).thenAnswer(inv -> inv.getArgument(0));

        byte[] data = new byte[]{1, 2, 3, 4, 5};
        dbFileStorageService.store(100L, 10L, data);

        ArgumentCaptor<DocumentFile> captor = ArgumentCaptor.forClass(DocumentFile.class);
        verify(fileRepository).save(captor.capture());

        DocumentFile file = captor.getValue();
        assertEquals(100L, file.getDocumentId());
        assertEquals(10L, file.getTenantId());
        assertArrayEquals(data, file.getData());
        assertEquals(5L, file.getFileSize());
    }

    @Test
    void store_nullData_doesNothing() {
        dbFileStorageService.store(100L, 10L, null);

        verifyNoInteractions(fileRepository);
    }

    @Test
    void store_emptyData_storesEmptyArray() {
        when(fileRepository.save(any(DocumentFile.class))).thenAnswer(inv -> inv.getArgument(0));

        byte[] empty = new byte[0];
        dbFileStorageService.store(100L, 10L, empty);

        ArgumentCaptor<DocumentFile> captor = ArgumentCaptor.forClass(DocumentFile.class);
        verify(fileRepository).save(captor.capture());

        assertArrayEquals(empty, captor.getValue().getData());
        assertEquals(0L, captor.getValue().getFileSize());
    }

    @Test
    void store_largeData_storesCorrectSize() {
        when(fileRepository.save(any(DocumentFile.class))).thenAnswer(inv -> inv.getArgument(0));

        byte[] largeData = new byte[1024 * 1024]; // 1 MB
        dbFileStorageService.store(200L, 10L, largeData);

        ArgumentCaptor<DocumentFile> captor = ArgumentCaptor.forClass(DocumentFile.class);
        verify(fileRepository).save(captor.capture());

        assertEquals(1024 * 1024L, captor.getValue().getFileSize());
    }

    // ---------- load ----------

    @Test
    void load_found() {
        byte[] data = new byte[]{10, 20, 30};
        DocumentFile file = DocumentFile.builder().documentId(100L).data(data).build();
        when(fileRepository.findById(100L)).thenReturn(Optional.of(file));

        byte[] result = dbFileStorageService.load(100L);
        assertArrayEquals(data, result);
    }

    @Test
    void load_notFound_returnsNull() {
        when(fileRepository.findById(999L)).thenReturn(Optional.empty());

        byte[] result = dbFileStorageService.load(999L);
        assertNull(result);
    }

    // ---------- delete ----------

    @Test
    void delete_success() {
        doNothing().when(fileRepository).deleteByDocumentId(100L);

        assertDoesNotThrow(() -> dbFileStorageService.delete(100L));
        verify(fileRepository).deleteByDocumentId(100L);
    }

    @Test
    void delete_exceptionSwallowed() {
        doThrow(new RuntimeException("DB error")).when(fileRepository).deleteByDocumentId(100L);

        // Should not propagate — warning logged internally
        assertDoesNotThrow(() -> dbFileStorageService.delete(100L));
        verify(fileRepository).deleteByDocumentId(100L);
    }
}
