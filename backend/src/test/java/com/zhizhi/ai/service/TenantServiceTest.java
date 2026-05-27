package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.model.entity.Tenant;
import com.zhizhi.ai.model.entity.TenantMember;
import com.zhizhi.ai.model.entity.User;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.TenantRepository;
import com.zhizhi.ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantService 单元测试")
class TenantServiceTest {

    @InjectMocks
    private TenantService tenantService;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantMemberRepository tenantMemberRepository;

    @Mock
    private UserRepository userRepository;

    private User testUser;
    private Tenant testTenant;
    private TenantMember ownerMember;
    private final Long USER_ID = 1L;
    private final Long OTHER_USER_ID = 2L;
    private final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(USER_ID).username("testuser").build();

        testTenant = Tenant.builder()
                .id(TENANT_ID)
                .name("测试商户")
                .plan("free")
                .status("active")
                .maxDocuments(10)
                .maxDailyQueries(100)
                .build();

        ownerMember = TenantMember.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .role("owner")
                .build();
    }

    @Nested
    @DisplayName("创建租户")
    class Create {

        @Test
        @DisplayName("创建成功，创建者自动成为owner")
        void create_success() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(tenantRepository.save(any(Tenant.class))).thenReturn(testTenant);
            when(tenantMemberRepository.save(any(TenantMember.class))).thenReturn(ownerMember);

            Tenant result = tenantService.create("测试商户", USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("测试商户");
            verify(tenantRepository).save(any(Tenant.class));
            verify(tenantMemberRepository).save(any(TenantMember.class));
        }

        @Test
        @DisplayName("用户不存在")
        void create_userNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.create("测试商户", 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(404));
        }
    }

    @Nested
    @DisplayName("获取租户")
    class GetById {

        @Test
        @DisplayName("获取成功")
        void getById_success() {
            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(testTenant));

            Tenant result = tenantService.getById(TENANT_ID);

            assertThat(result.getName()).isEqualTo("测试商户");
        }

        @Test
        @DisplayName("租户不存在")
        void getById_notFound() {
            when(tenantRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.getById(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("获取用户租户")
    class GetByUserId {

        @Test
        @DisplayName("通过用户ID获取租户")
        void getByUserId_success() {
            when(tenantMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(ownerMember));
            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(testTenant));

            Tenant result = tenantService.getByUserId(USER_ID);

            assertThat(result.getName()).isEqualTo("测试商户");
        }

        @Test
        @DisplayName("用户无租户")
        void getByUserId_noMember() {
            when(tenantMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.getByUserId(USER_ID))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("添加成员")
    class AddMember {

        @Test
        @DisplayName("添加成功")
        void addMember_success() {
            User newUser = User.builder().id(OTHER_USER_ID).username("newuser").build();
            when(tenantMemberRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
                    .thenReturn(Optional.of(ownerMember));
            when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(newUser));
            when(tenantMemberRepository.existsByTenantIdAndUserId(TENANT_ID, OTHER_USER_ID))
                    .thenReturn(false);
            when(tenantMemberRepository.save(any(TenantMember.class)))
                    .thenReturn(TenantMember.builder().id(2L).tenantId(TENANT_ID).userId(OTHER_USER_ID).role("member").build());

            TenantMember result = tenantService.addMember(TENANT_ID, "newuser", "member", USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getRole()).isEqualTo("member");
            verify(tenantMemberRepository).save(any(TenantMember.class));
        }

        @Test
        @DisplayName("重复添加")
        void addMember_duplicate() {
            when(tenantMemberRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
                    .thenReturn(Optional.of(ownerMember));
            when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(testUser));
            when(tenantMemberRepository.existsByTenantIdAndUserId(TENANT_ID, USER_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> tenantService.addMember(TENANT_ID, "newuser", "member", USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(400));
        }

        @Test
        @DisplayName("非管理员无权添加")
        void addMember_forbidden() {
            TenantMember memberRole = TenantMember.builder()
                    .tenantId(TENANT_ID).userId(OTHER_USER_ID).role("member").build();
            when(tenantMemberRepository.findByTenantIdAndUserId(TENANT_ID, OTHER_USER_ID))
                    .thenReturn(Optional.of(memberRole));

            assertThatThrownBy(() -> tenantService.addMember(TENANT_ID, "newuser", "member", OTHER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(403));
        }
    }

    @Nested
    @DisplayName("成员检查")
    class IsMember {

        @Test
        @DisplayName("是成员")
        void isMember_true() {
            when(tenantMemberRepository.existsByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(true);

            assertThat(tenantService.isMember(TENANT_ID, USER_ID)).isTrue();
        }

        @Test
        @DisplayName("不是成员")
        void isMember_false() {
            when(tenantMemberRepository.existsByTenantIdAndUserId(TENANT_ID, OTHER_USER_ID)).thenReturn(false);

            assertThat(tenantService.isMember(TENANT_ID, OTHER_USER_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("获取成员列表")
    class GetMembers {

        @Test
        @DisplayName("获取成功")
        void getMembers_success() {
            when(tenantMemberRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
                    .thenReturn(Optional.of(ownerMember));
            when(tenantMemberRepository.findByTenantId(TENANT_ID))
                    .thenReturn(List.of(ownerMember));

            List<TenantMember> result = tenantService.getMembers(TENANT_ID, USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRole()).isEqualTo("owner");
        }
    }
}
