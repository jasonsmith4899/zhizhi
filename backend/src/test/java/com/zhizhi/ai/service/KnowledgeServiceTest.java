package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.model.dto.KnowledgeBaseRequest;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.repository.DocumentRepository;
import com.zhizhi.ai.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeService 单元测试")
class KnowledgeServiceTest {

    @InjectMocks
    private KnowledgeService knowledgeService;

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private DocumentRepository documentRepository;

    private KnowledgeBase testKb;
    private KnowledgeBaseRequest kbRequest;
    private final Long USER_ID = 1L;
    private final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        testKb = KnowledgeBase.builder()
                .id(1L)
                .userId(USER_ID)
                .name("测试知识库")
                .description("测试描述")
                .status("active")
                .documentCount(5)
                .chunkCount(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        kbRequest = new KnowledgeBaseRequest();
        kbRequest.setName("新知识库");
        kbRequest.setDescription("新描述");
    }

    @Nested
    @DisplayName("创建知识库")
    class Create {

        @Test
        @DisplayName("创建成功")
        void create_success() {
            when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenReturn(testKb);

            KnowledgeBase result = knowledgeService.create(kbRequest, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("测试知识库");
            verify(knowledgeBaseRepository).save(any(KnowledgeBase.class));
        }
    }

    @Nested
    @DisplayName("获取知识库")
    class GetById {

        @Test
        @DisplayName("正常获取")
        void getById_success() {
            when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(testKb));

            KnowledgeBase result = knowledgeService.getById(1L, USER_ID);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("测试知识库");
        }

        @Test
        @DisplayName("知识库不存在")
        void getById_notFound() {
            when(knowledgeBaseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> knowledgeService.getById(999L, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(404));
        }

        @Test
        @DisplayName("无权访问他人知识库")
        void getById_forbidden() {
            when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(testKb));

            assertThatThrownBy(() -> knowledgeService.getById(1L, OTHER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(403));
        }
    }

    @Nested
    @DisplayName("获取用户知识库列表")
    class ListByUser {

        @Test
        @DisplayName("返回列表")
        void listByUser_success() {
            when(knowledgeBaseRepository.findByUserIdOrderByUpdatedAtDesc(USER_ID))
                    .thenReturn(List.of(testKb));

            List<KnowledgeBase> result = knowledgeService.listByUser(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("测试知识库");
        }

        @Test
        @DisplayName("空列表")
        void listByUser_empty() {
            when(knowledgeBaseRepository.findByUserIdOrderByUpdatedAtDesc(USER_ID))
                    .thenReturn(List.of());

            List<KnowledgeBase> result = knowledgeService.listByUser(USER_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("更新知识库")
    class Update {

        @Test
        @DisplayName("更新成功")
        void update_success() {
            when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(testKb));
            when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenReturn(testKb);

            KnowledgeBase result = knowledgeService.update(1L, kbRequest, USER_ID);

            assertThat(result).isNotNull();
            verify(knowledgeBaseRepository).save(any(KnowledgeBase.class));
        }

        @Test
        @DisplayName("无权更新")
        void update_forbidden() {
            when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(testKb));

            assertThatThrownBy(() -> knowledgeService.update(1L, kbRequest, OTHER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(403));
        }
    }

    @Nested
    @DisplayName("删除知识库")
    class Delete {

        @Test
        @DisplayName("删除成功")
        void delete_success() {
            when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(testKb));
            doNothing().when(knowledgeBaseRepository).delete(any());

            knowledgeService.delete(1L, USER_ID);

            verify(knowledgeBaseRepository).delete(testKb);
        }

        @Test
        @DisplayName("无权删除")
        void delete_forbidden() {
            when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(testKb));

            assertThatThrownBy(() -> knowledgeService.delete(1L, OTHER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(403));
        }
    }
}
