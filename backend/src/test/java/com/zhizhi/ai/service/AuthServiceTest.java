package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.model.dto.LoginRequest;
import com.zhizhi.ai.model.dto.RegisterRequest;
import com.zhizhi.ai.model.entity.User;
import com.zhizhi.ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("$2a$10$encodedPassword")
                .plan("free")
                .apiKey("test-api-key")
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("123456");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("123456");
    }

    @Nested
    @DisplayName("注册")
    class Register {

        @Test
        @DisplayName("注册成功")
        void register_success() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("123456")).thenReturn("$2a$10$encoded");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtil.generateToken(anyLong(), anyString())).thenReturn("jwt-token");

            Map<String, Object> result = authService.register(registerRequest);

            assertThat(result).containsKey("token");
            assertThat(result).containsKey("user");
            assertThat(result.get("token")).isEqualTo("jwt-token");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("用户名已存在")
        void register_duplicateUsername() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(400));
        }

        @Test
        @DisplayName("邮箱已存在")
        void register_duplicateEmail() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("登录")
    class Login {

        @Test
        @DisplayName("登录成功")
        void login_success() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("123456", "$2a$10$encodedPassword")).thenReturn(true);
            when(jwtUtil.generateToken(anyLong(), anyString())).thenReturn("jwt-token");

            Map<String, Object> result = authService.login(loginRequest);

            assertThat(result).containsKey("token");
            assertThat(result).containsKey("user");
        }

        @Test
        @DisplayName("用户不存在")
        void login_userNotFound() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(401));
        }

        @Test
        @DisplayName("密码错误")
        void login_wrongPassword() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("123456", "$2a$10$encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(401));
        }
    }

    @Nested
    @DisplayName("获取当前用户")
    class GetCurrentUser {

        @Test
        @DisplayName("成功获取")
        void getCurrentUser_success() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            User result = authService.getCurrentUser("testuser");

            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("用户不存在")
        void getCurrentUser_notFound() {
            when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUser("nobody"))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
