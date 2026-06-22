package com.zhizhi.ai.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 文档原始文件字节（与 Document 一对一，共享主键）。
 * 独立成表，避免大字段拖累 documents 的常规查询。
 */
@Entity
@Table(name = "document_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentFile {

    @Id
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(columnDefinition = "bytea", nullable = false)
    private byte[] data;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
