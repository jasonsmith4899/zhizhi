package com.zhizhi.ai.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

/**
 * 文档-标签关联（多对多联结表，复合主键）
 */
@Entity
@Table(name = "document_tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(DocumentTag.PK.class)
public class DocumentTag {

    @Id
    @Column(name = "document_id")
    private Long documentId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {
        private Long documentId;
        private Long tagId;
    }
}
