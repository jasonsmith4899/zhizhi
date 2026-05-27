package com.zhizhi.ai.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentListDTO {

    private Long id;
    private String filename;
    private String fileType;
    private Long fileSize;
    private String status;
    private Integer chunkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentListDTO fromEntity(com.zhizhi.ai.model.entity.Document doc) {
        return DocumentListDTO.builder()
                .id(doc.getId())
                .filename(doc.getFilename())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus())
                .chunkCount(doc.getChunkCount())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
