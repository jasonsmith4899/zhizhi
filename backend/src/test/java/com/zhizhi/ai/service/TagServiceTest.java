package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.DocumentTag;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.model.entity.Tag;
import com.zhizhi.ai.repository.DocumentTagRepository;
import com.zhizhi.ai.repository.TagRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagService 单元测试")
class TagServiceTest {

    @Mock private TagRepository tagRepository;
    @Mock private DocumentTagRepository documentTagRepository;
    @Mock private KnowledgeService knowledgeService;

    @InjectMocks
    private TagService tagService;

    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long KB_ID = 10L;

    private KnowledgeBase sampleKb;

    @BeforeEach
    void setUp() {
        sampleKb = KnowledgeBase.builder()
                .id(KB_ID).userId(USER_ID).tenantId(TENANT_ID).name("KB").build();
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    // ==================== create ====================

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("创建新标签成功")
        void create_newTag() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);
            when(tagRepository.findByTenantIdAndKnowledgeBaseIdAndName(TENANT_ID, KB_ID, "Important"))
                    .thenReturn(Optional.empty());
            when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
                Tag t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            Tag result = tagService.create(KB_ID, "Important", "#ff0000", USER_ID);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Important");
            assertThat(result.getColor()).isEqualTo("#ff0000");
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            verify(tagRepository).save(any(Tag.class));
        }

        @Test
        @DisplayName("标签已存在 - 返回已有标签（幂等）")
        void create_existingTag() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);
            Tag existing = Tag.builder().id(1L).tenantId(TENANT_ID).knowledgeBaseId(KB_ID)
                    .name("Important").color("#ff0000").build();
            when(tagRepository.findByTenantIdAndKnowledgeBaseIdAndName(TENANT_ID, KB_ID, "Important"))
                    .thenReturn(Optional.of(existing));

            Tag result = tagService.create(KB_ID, "Important", "#00ff00", USER_ID);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getColor()).isEqualTo("#ff0000"); // 原有颜色不变
            verify(tagRepository, never()).save(any());
        }
    }

    // ==================== list ====================

    @Nested
    @DisplayName("list")
    class ListTests {

        @Test
        @DisplayName("列出知识库下所有标签")
        void list_success() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);
            Tag t1 = Tag.builder().id(1L).name("Alpha").build();
            Tag t2 = Tag.builder().id(2L).name("Beta").build();
            when(tagRepository.findByTenantIdAndKnowledgeBaseIdOrderByName(TENANT_ID, KB_ID))
                    .thenReturn(List.of(t1, t2));

            List<Tag> result = tagService.list(KB_ID, USER_ID);

            assertThat(result).hasSize(2);
        }
    }

    // ==================== delete ====================

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("删除成功 - 同时清理文档关联")
        void delete_success() {
            Tag tag = Tag.builder().id(1L).knowledgeBaseId(KB_ID).name("Old").build();
            when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);

            tagService.delete(1L, USER_ID);

            verify(documentTagRepository).deleteByTagId(1L);
            verify(tagRepository).delete(tag);
        }

        @Test
        @DisplayName("删除失败 - 标签不存在")
        void delete_notFound() {
            when(tagRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tagService.delete(99L, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
        }
    }

    // ==================== setDocumentTags ====================

    @Nested
    @DisplayName("setDocumentTags")
    class SetDocumentTagsTests {

        @Test
        @DisplayName("设置标签成功 - 全量覆盖")
        void setDocumentTags_success() {
            Tag tag1 = Tag.builder().id(1L).tenantId(TENANT_ID).build();
            Tag tag2 = Tag.builder().id(2L).tenantId(TENANT_ID).build();
            when(tagRepository.findById(1L)).thenReturn(Optional.of(tag1));
            when(tagRepository.findById(2L)).thenReturn(Optional.of(tag2));

            tagService.setDocumentTags(100L, List.of(1L, 2L), TENANT_ID);

            verify(documentTagRepository).deleteByDocumentId(100L);
            verify(documentTagRepository, times(2)).save(any(DocumentTag.class));
        }

        @Test
        @DisplayName("清空标签")
        void setDocumentTags_clear() {
            tagService.setDocumentTags(100L, List.of(), TENANT_ID);

            verify(documentTagRepository).deleteByDocumentId(100L);
            verify(documentTagRepository, never()).save(any());
        }

        @Test
        @DisplayName("传入null也清空标签")
        void setDocumentTags_null() {
            tagService.setDocumentTags(100L, null, TENANT_ID);

            verify(documentTagRepository).deleteByDocumentId(100L);
            verify(documentTagRepository, never()).save(any());
        }

        @Test
        @DisplayName("去重 - 相同tagId只保存一次")
        void setDocumentTags_dedup() {
            Tag tag1 = Tag.builder().id(1L).tenantId(TENANT_ID).build();
            when(tagRepository.findById(1L)).thenReturn(Optional.of(tag1));

            tagService.setDocumentTags(100L, List.of(1L, 1L, 1L), TENANT_ID);

            verify(documentTagRepository, times(1)).save(any(DocumentTag.class));
        }

        @Test
        @DisplayName("跨租户标签 - 抛出禁止异常")
        void setDocumentTags_crossTenant() {
            Tag otherTag = Tag.builder().id(1L).tenantId(999L).build();
            when(tagRepository.findById(1L)).thenReturn(Optional.of(otherTag));

            assertThatThrownBy(() -> tagService.setDocumentTags(100L, List.of(1L), TENANT_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
        }

        @Test
        @DisplayName("标签不存在 - 静默跳过")
        void setDocumentTags_tagNotFound() {
            when(tagRepository.findById(99L)).thenReturn(Optional.empty());

            tagService.setDocumentTags(100L, List.of(99L), TENANT_ID);

            verify(documentTagRepository).deleteByDocumentId(100L);
            verify(documentTagRepository, never()).save(any());
        }
    }

    // ==================== getDocumentTagIds ====================

    @Nested
    @DisplayName("getDocumentTagIds")
    class GetDocumentTagIdsTests {

        @Test
        @DisplayName("返回文档的标签ID列表")
        void getDocumentTagIds_success() {
            when(documentTagRepository.findTagIdsByDocumentId(100L))
                    .thenReturn(List.of(1L, 2L, 3L));

            List<Long> result = tagService.getDocumentTagIds(100L);

            assertThat(result).containsExactly(1L, 2L, 3L);
        }

        @Test
        @DisplayName("无标签时返回空列表")
        void getDocumentTagIds_empty() {
            when(documentTagRepository.findTagIdsByDocumentId(100L))
                    .thenReturn(List.of());

            assertThat(tagService.getDocumentTagIds(100L)).isEmpty();
        }
    }
}
