package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.dto.KnowledgeBaseRequest;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.repository.DocumentRepository;
import com.zhizhi.ai.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeService 单元测试")
class KnowledgeServiceTest {

    @Mock private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private KnowledgeService knowledgeService;

    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long KB_ID = 10L;

    private KnowledgeBase sampleKb;

    @BeforeEach
    void setUp() {
        sampleKb = KnowledgeBase.builder()
                .id(KB_ID)
                .userId(USER_ID)
                .tenantId(TENANT_ID)
                .name("Test KB")
                .description("A test knowledge base")
                .status("active")
                .documentCount(0)
                .chunkCount(0)
                .build();
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
        @DisplayName("创建成功")
        void create_success() {
            TenantContext.setTenantId(TENANT_ID);
            KnowledgeBaseRequest request = new KnowledgeBaseRequest();
            request.setName("New KB");
            request.setDescription("desc");

            when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(inv -> {
                KnowledgeBase kb = inv.getArgument(0);
                kb.setId(20L);
                return kb;
            });

            KnowledgeBase result = knowledgeService.create(request, USER_ID);

            assertThat(result.getId()).isEqualTo(20L);
            assertThat(result.getName()).isEqualTo("New KB");
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            verify(knowledgeBaseRepository).save(any(KnowledgeBase.class));
        }

        @Test
        @DisplayName("创建失败 - 缺少租户信息")
        void create_noTenant() {
            TenantContext.clear();
            KnowledgeBaseRequest request = new KnowledgeBaseRequest();
            request.setName("New KB");

            assertThatThrownBy(() -> knowledgeService.create(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("租户");
        }
    }

    // ==================== getById ====================

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("获取成功")
        void getById_success() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));

            KnowledgeBase result = knowledgeService.getById(KB_ID, USER_ID);
            assertThat(result).isEqualTo(sampleKb);
        }

        @Test
        @DisplayName("获取失败 - 知识库不存在")
        void getById_notFound() {
            when(knowledgeBaseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> knowledgeService.getById(999L, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
        }

        @Test
        @DisplayName("获取失败 - 租户不匹配")
        void getById_tenantMismatch() {
            TenantContext.setTenantId(999L);
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));

            assertThatThrownBy(() -> knowledgeService.getById(KB_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
        }

        @Test
        @DisplayName("获取失败 - 用户不匹配")
        void getById_userMismatch() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));

            assertThatThrownBy(() -> knowledgeService.getById(KB_ID, 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
        }
    }

    // ==================== listByUser ====================

    @Nested
    @DisplayName("listByUser")
    class ListByUserTests {

        @Test
        @DisplayName("按租户查询（有租户ID）")
        void listByUser_withTenant() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeBaseRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT_ID))
                    .thenReturn(List.of(sampleKb));

            List<KnowledgeBase> result = knowledgeService.listByUser(USER_ID);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("按用户查询（无租户ID）")
        void listByUser_noTenant() {
            TenantContext.clear();
            when(knowledgeBaseRepository.findByUserIdOrderByUpdatedAtDesc(USER_ID))
                    .thenReturn(List.of(sampleKb));

            List<KnowledgeBase> result = knowledgeService.listByUser(USER_ID);
            assertThat(result).hasSize(1);
        }
    }

    // ==================== update ====================

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("更新成功")
        void update_success() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(inv -> inv.getArgument(0));

            KnowledgeBaseRequest request = new KnowledgeBaseRequest();
            request.setName("Updated Name");
            request.setDescription("Updated desc");

            KnowledgeBase result = knowledgeService.update(KB_ID, request, USER_ID);

            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getDescription()).isEqualTo("Updated desc");
            verify(knowledgeBaseRepository).save(sampleKb);
        }
    }

    // ==================== delete ====================

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("删除成功 - 级联清理所有关联数据")
        void delete_success() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));

            knowledgeService.delete(KB_ID, USER_ID);

            // Verify cascading deletes in order
            verify(jdbcTemplate).update(contains("DELETE FROM messages"), eq(TENANT_ID), eq(KB_ID));
            verify(jdbcTemplate).update(contains("spring_ai_chat_memory"), eq(KB_ID), eq(TENANT_ID));
            verify(jdbcTemplate).update(contains("DELETE FROM conversations"), eq(KB_ID), eq(TENANT_ID));
            verify(jdbcTemplate).update(contains("DELETE FROM vector_chunks"), eq(KB_ID.toString()), eq(TENANT_ID.toString()));
            verify(jdbcTemplate).update(contains("DELETE FROM document_chunks"), eq(KB_ID), eq(TENANT_ID));
            verify(jdbcTemplate).update(contains("DELETE FROM document_tags"), eq(KB_ID), eq(TENANT_ID));
            verify(jdbcTemplate).update(contains("DELETE FROM document_versions"), eq(KB_ID), eq(TENANT_ID));
            verify(jdbcTemplate).update(contains("DELETE FROM documents"), eq(KB_ID), eq(TENANT_ID));
            verify(jdbcTemplate).update(contains("DELETE FROM tags"), eq(KB_ID), eq(TENANT_ID));
            verify(jdbcTemplate).update(contains("DELETE FROM categories"), eq(KB_ID), eq(TENANT_ID));
            verify(jdbcTemplate).update(contains("DELETE FROM kg_relations"), eq(KB_ID), eq(TENANT_ID));
            verify(jdbcTemplate).update(contains("DELETE FROM kg_entities"), eq(KB_ID), eq(TENANT_ID));
            verify(knowledgeBaseRepository).delete(sampleKb);
        }

        @Test
        @DisplayName("删除失败 - 知识库不存在")
        void delete_notFound() {
            when(knowledgeBaseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> knowledgeService.delete(999L, USER_ID))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
