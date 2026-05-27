package com.zhizhi.ai.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 文档切片实体（向量存储在pgvector中，此处仅存元数据）
 * 注意：向量数据由Spring AI的PgVectorStore自动管理
 */
@Entity
@Table(name = "document_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "knowledge_base_id", nullable = false)
    private Long knowledgeBaseId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "content_length")
    private Integer contentLength;

    @Column(name = "vector_id", length = 100)
    private String vectorId;  // pgvector中的向量ID

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (content != null) {
            contentLength = content.length();
        }
    }
}
