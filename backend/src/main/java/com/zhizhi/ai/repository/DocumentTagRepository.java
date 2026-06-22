package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.DocumentTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentTagRepository extends JpaRepository<DocumentTag, DocumentTag.PK> {

    List<DocumentTag> findByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);

    void deleteByTagId(Long tagId);

    /** 查带有指定标签集合的文档 ID（用于检索过滤） */
    @Query("SELECT DISTINCT dt.documentId FROM DocumentTag dt WHERE dt.tenantId = :tenantId AND dt.tagId IN :tagIds")
    List<Long> findDocumentIdsByTagIds(@Param("tenantId") Long tenantId, @Param("tagIds") List<Long> tagIds);

    /** 查文档的标签 ID 列表 */
    @Query("SELECT dt.tagId FROM DocumentTag dt WHERE dt.documentId = :documentId")
    List<Long> findTagIdsByDocumentId(@Param("documentId") Long documentId);
}
