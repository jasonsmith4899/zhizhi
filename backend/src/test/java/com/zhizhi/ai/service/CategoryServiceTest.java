package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.Category;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.repository.CategoryRepository;
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
@DisplayName("CategoryService 单元测试")
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private KnowledgeService knowledgeService;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CategoryService categoryService;

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
        @DisplayName("创建根分类成功")
        void create_rootCategory() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });

            Category result = categoryService.create(KB_ID, null, "Root", 0, USER_ID);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Root");
            assertThat(result.getParentId()).isNull();
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.getKnowledgeBaseId()).isEqualTo(KB_ID);
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("创建子分类成功")
        void create_childCategory() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);
            Category parent = Category.builder().id(1L).knowledgeBaseId(KB_ID).build();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(2L);
                return c;
            });

            Category result = categoryService.create(KB_ID, 1L, "Child", 1, USER_ID);

            assertThat(result.getParentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("创建子分类失败 - 父分类不属于该知识库")
        void create_parentNotInKb() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);
            Category otherParent = Category.builder().id(1L).knowledgeBaseId(999L).build();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(otherParent));

            assertThatThrownBy(() -> categoryService.create(KB_ID, 1L, "Child", 0, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不属于该知识库");
        }

        @Test
        @DisplayName("sortOrder为null时默认为0")
        void create_defaultSortOrder() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

            Category result = categoryService.create(KB_ID, null, "Cat", null, USER_ID);

            assertThat(result.getSortOrder()).isEqualTo(0);
        }
    }

    // ==================== list ====================

    @Nested
    @DisplayName("list")
    class ListTests {

        @Test
        @DisplayName("列出知识库下所有分类")
        void list_success() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);
            Category c1 = Category.builder().id(1L).name("A").sortOrder(0).build();
            Category c2 = Category.builder().id(2L).name("B").sortOrder(1).build();
            when(categoryRepository.findByTenantIdAndKnowledgeBaseIdOrderBySortOrderAscIdAsc(TENANT_ID, KB_ID))
                    .thenReturn(List.of(c1, c2));

            List<Category> result = categoryService.list(KB_ID, USER_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("A");
        }
    }

    // ==================== update ====================

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("更新名称和排序")
        void update_success() {
            Category existing = Category.builder().id(1L).knowledgeBaseId(KB_ID).name("Old").sortOrder(0).build();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

            Category result = categoryService.update(1L, "New", null, 5, USER_ID);

            assertThat(result.getName()).isEqualTo("New");
            assertThat(result.getSortOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("更新失败 - 分类不存在")
        void update_notFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.update(99L, "X", null, null, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
        }

        @Test
        @DisplayName("更新失败 - 不能以自身作为父级")
        void update_selfParent() {
            Category existing = Category.builder().id(1L).knowledgeBaseId(KB_ID).name("Cat").build();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);

            assertThatThrownBy(() -> categoryService.update(1L, "Cat", 1L, null, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("自身");
        }

        @Test
        @DisplayName("更新失败 - 父分类不属于该知识库")
        void update_parentNotInKb() {
            Category existing = Category.builder().id(1L).knowledgeBaseId(KB_ID).name("Cat").build();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);
            Category otherParent = Category.builder().id(2L).knowledgeBaseId(999L).build();
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(otherParent));

            assertThatThrownBy(() -> categoryService.update(1L, "Cat", 2L, null, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不属于该知识库");
        }
    }

    // ==================== delete ====================

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("删除成功 - 叶子节点")
        void delete_success() {
            Category leaf = Category.builder().id(1L).knowledgeBaseId(KB_ID).name("Leaf").build();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(leaf));
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);
            when(categoryRepository.countByParentId(1L)).thenReturn(0L);

            categoryService.delete(1L, USER_ID);

            verify(jdbcTemplate).update("UPDATE documents SET category_id = NULL WHERE category_id = ?", 1L);
            verify(categoryRepository).delete(leaf);
        }

        @Test
        @DisplayName("删除失败 - 有子分类")
        void delete_hasChildren() {
            Category parent = Category.builder().id(1L).knowledgeBaseId(KB_ID).name("Parent").build();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
            when(knowledgeService.getById(KB_ID, USER_ID)).thenReturn(sampleKb);
            when(categoryRepository.countByParentId(1L)).thenReturn(3L);

            assertThatThrownBy(() -> categoryService.delete(1L, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("子分类");
        }

        @Test
        @DisplayName("删除失败 - 分类不存在")
        void delete_notFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.delete(99L, USER_ID))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
