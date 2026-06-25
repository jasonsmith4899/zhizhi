package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.model.dto.LoginRequest;
import com.zhizhi.ai.model.dto.RegisterRequest;
import com.zhizhi.ai.model.dto.UserDTO;
import com.zhizhi.ai.model.entity.ApiKey;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.model.entity.User;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.KnowledgeBaseRepository;
import com.zhizhi.ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ApiKeyRepository apiKeyRepository;
    @Mock private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPass")
                .plan("free")
                .refreshTokenVersion(0)
                .dailyQueriesUsed(0)
                .build();
    }

    // ==================== register ====================

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("注册成功 - 返回token和用户信息")
        void register_success() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("password123");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(2L);
                return u;
            });
            when(jwtUtil.generateToken(eq(2L), eq("newuser"))).thenReturn("access-token");
            when(jwtUtil.generateRefreshToken(eq(2L), eq(0))).thenReturn("refresh-token");

            Map<String, Object> result = authService.register(request);

            assertThat(result).containsKeys("token", "refreshToken", "user");
            assertThat(result.get("token")).isEqualTo("access-token");
            assertThat(result.get("refreshToken")).isEqualTo("refresh-token");
            assertThat(result.get("user")).isInstanceOf(UserDTO.class);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("注册失败 - 用户名已存在")
        void register_usernameExists() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("existinguser");
            request.setEmail("new@example.com");
            request.setPassword("password123");

            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名已存在");
        }

        @Test
        @DisplayName("注册失败 - 邮箱已被注册")
        void register_emailExists() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("existing@example.com");
            request.setPassword("password123");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("邮箱已被注册");
        }
    }

    // ==================== login ====================

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("登录成功")
        void login_success() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("rawPass");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("rawPass", "encodedPass")).thenReturn(true);
            when(jwtUtil.generateToken(1L, "testuser")).thenReturn("access-token");
            when(jwtUtil.generateRefreshToken(1L, 0)).thenReturn("refresh-token");

            Map<String, Object> result = authService.login(request);

            assertThat(result.get("token")).isEqualTo("access-token");
            assertThat(result.get("refreshToken")).isEqualTo("refresh-token");
            assertThat(result.get("user")).isInstanceOf(UserDTO.class);
        }

        @Test
        @DisplayName("登录失败 - 用户不存在")
        void login_userNotFound() {
            LoginRequest request = new LoginRequest();
            request.setUsername("nobody");
            request.setPassword("pass");

            when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(401));
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void login_wrongPassword() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrong");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("wrong", "encodedPass")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(401))
                    .hasMessageContaining("用户名或密码错误");
        }
    }

    // ==================== refreshToken ====================

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("刷新成功 - token轮换")
        void refreshToken_success() {
            String oldRefreshToken = "old-refresh";

            when(jwtUtil.validateRefreshToken(oldRefreshToken)).thenReturn(true);
            when(jwtUtil.getUserId(oldRefreshToken)).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(jwtUtil.getRefreshTokenVersion(oldRefreshToken)).thenReturn(0);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtUtil.generateToken(1L, "testuser")).thenReturn("new-access");
            when(jwtUtil.generateRefreshToken(1L, 1)).thenReturn("new-refresh");

            Map<String, Object> result = authService.refreshToken(oldRefreshToken);

            assertThat(result.get("token")).isEqualTo("new-access");
            assertThat(result.get("refreshToken")).isEqualTo("new-refresh");
            // verify version was bumped
            assertThat(sampleUser.getRefreshTokenVersion()).isEqualTo(1);
            verify(userRepository).save(sampleUser);
        }

        @Test
        @DisplayName("刷新失败 - token无效")
        void refreshToken_invalid() {
            when(jwtUtil.validateRefreshToken("bad")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken("bad"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(401));
        }

        @Test
        @DisplayName("刷新失败 - 用户不存在")
        void refreshToken_userNotFound() {
            when(jwtUtil.validateRefreshToken("token")).thenReturn(true);
            when(jwtUtil.getUserId("token")).thenReturn(999L);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken("token"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(401));
        }

        @Test
        @DisplayName("刷新失败 - token版本过期")
        void refreshToken_versionExpired() {
            sampleUser.setRefreshTokenVersion(5);
            String oldRefreshToken = "stale-refresh";

            when(jwtUtil.validateRefreshToken(oldRefreshToken)).thenReturn(true);
            when(jwtUtil.getUserId(oldRefreshToken)).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(jwtUtil.getRefreshTokenVersion(oldRefreshToken)).thenReturn(3);

            assertThatThrownBy(() -> authService.refreshToken(oldRefreshToken))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(401))
                    .hasMessageContaining("已失效");
        }
    }

    // ==================== getCurrentUser ====================

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUserTests {

        @Test
        @DisplayName("获取成功")
        void getCurrentUser_found() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));

            User result = authService.getCurrentUser("testuser");
            assertThat(result).isEqualTo(sampleUser);
        }

        @Test
        @DisplayName("获取失败 - 用户不存在")
        void getCurrentUser_notFound() {
            when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUser("nobody"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
        }
    }

    // ==================== listApiKeys ====================

    @Nested
    @DisplayName("listApiKeys")
    class ListApiKeysTests {

        @Test
        @DisplayName("返回用户的所有API Key（脱敏）")
        void listApiKeys_success() {
            ApiKey key = ApiKey.builder().id(1L).userId(1L).keyValue("abcdef123456").name("Key1").build();
            Page<ApiKey> page = new PageImpl<>(List.of(key));

            when(apiKeyRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                    .thenReturn(page);

            var result = authService.listApiKeys(1L);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("无API Key时返回空列表")
        void listApiKeys_empty() {
            when(apiKeyRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThat(authService.listApiKeys(1L)).isEmpty();
        }
    }

    // ==================== createApiKey ====================

    @Nested
    @DisplayName("createApiKey")
    class CreateApiKeyTests {

        @Test
        @DisplayName("创建成功 - 不关联知识库")
        void createApiKey_noKbs() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
                ApiKey k = inv.getArgument(0);
                k.setId(10L);
                return k;
            });

            var result = authService.createApiKey(1L, "MyKey", "desc",
                    "persona", "bg", "rules", null);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            verify(apiKeyRepository).save(any(ApiKey.class));
        }

        @Test
        @DisplayName("创建成功 - 关联知识库（过滤非本人KB）")
        void createApiKey_withKbs_filtersInvalid() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            KnowledgeBase ownKb = KnowledgeBase.builder().id(100L).userId(1L).build();
            KnowledgeBase otherKb = KnowledgeBase.builder().id(200L).userId(2L).build();
            when(knowledgeBaseRepository.findAllById(Set.of(100L, 200L)))
                    .thenReturn(List.of(ownKb, otherKb));
            when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
                ApiKey k = inv.getArgument(0);
                k.setId(10L);
                return k;
            });

            var result = authService.createApiKey(1L, "Key", null,
                    null, null, null, Set.of(100L, 200L));

            assertThat(result).isNotNull();
            verify(apiKeyRepository).save(argThat(k -> k.getKnowledgeBases().size() == 1));
        }

        @Test
        @DisplayName("创建失败 - 用户不存在")
        void createApiKey_userNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.createApiKey(999L, "Key", null,
                    null, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
        }
    }

    // ==================== updateApiKey ====================

    @Nested
    @DisplayName("updateApiKey")
    class UpdateApiKeyTests {

        @Test
        @DisplayName("更新成功")
        void updateApiKey_success() {
            ApiKey existing = ApiKey.builder().id(10L).userId(1L).name("Old").build();
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = authService.updateApiKey(1L, 10L, "New", "newDesc",
                    "p", "bg", "rules", null);

            assertThat(result).isNotNull();
            assertThat(existing.getName()).isEqualTo("New");
            verify(apiKeyRepository).save(existing);
        }

        @Test
        @DisplayName("更新失败 - API Key不存在")
        void updateApiKey_notFound() {
            when(apiKeyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.updateApiKey(1L, 99L, "n", null,
                    null, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
        }

        @Test
        @DisplayName("更新失败 - 无权操作他人API Key")
        void updateApiKey_forbidden() {
            ApiKey other = ApiKey.builder().id(10L).userId(2L).build();
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> authService.updateApiKey(1L, 10L, "n", null,
                    null, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
        }
    }

    // ==================== deleteApiKey ====================

    @Nested
    @DisplayName("deleteApiKey")
    class DeleteApiKeyTests {

        @Test
        @DisplayName("删除成功")
        void deleteApiKey_success() {
            ApiKey key = ApiKey.builder().id(10L).userId(1L).build();
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(key));

            authService.deleteApiKey(1L, 10L);

            verify(apiKeyRepository).delete(key);
        }

        @Test
        @DisplayName("删除失败 - 无权操作他人API Key")
        void deleteApiKey_forbidden() {
            ApiKey other = ApiKey.builder().id(10L).userId(2L).build();
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> authService.deleteApiKey(1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
        }
    }

    // ==================== getApiKeyById / getApiKeyByValue ====================

    @Nested
    @DisplayName("getApiKeyById / getApiKeyByValue")
    class GetApiKeyTests {

        @Test
        @DisplayName("按ID查找成功")
        void getApiKeyById_found() {
            ApiKey key = ApiKey.builder().id(10L).build();
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(key));

            assertThat(authService.getApiKeyById(10L)).isEqualTo(key);
        }

        @Test
        @DisplayName("按ID查找失败")
        void getApiKeyById_notFound() {
            when(apiKeyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getApiKeyById(99L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("按value查找成功")
        void getApiKeyByValue_found() {
            ApiKey key = ApiKey.builder().keyValue("abc123").build();
            when(apiKeyRepository.findByKeyValue("abc123")).thenReturn(Optional.of(key));

            assertThat(authService.getApiKeyByValue("abc123")).isEqualTo(key);
        }

        @Test
        @DisplayName("按value查找 - 不存在返回null")
        void getApiKeyByValue_notFound() {
            when(apiKeyRepository.findByKeyValue("unknown")).thenReturn(Optional.empty());

            assertThat(authService.getApiKeyByValue("unknown")).isNull();
        }
    }
}
