package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.model.dto.TenantUpdateRequest;
import com.zhizhi.ai.model.entity.Tenant;
import com.zhizhi.ai.model.entity.TenantMember;
import com.zhizhi.ai.model.entity.User;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.TenantRepository;
import com.zhizhi.ai.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantMemberRepository tenantMemberRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TenantService tenantService;

    // ---------- create ----------

    @Test
    void create_success() {
        User user = User.builder().id(1L).username("alice").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Tenant savedTenant = Tenant.builder().id(10L).name("Acme").build();
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(tenantMemberRepository.save(any(TenantMember.class))).thenReturn(null);

        Tenant result = tenantService.create("Acme", 1L);

        assertEquals(10L, result.getId());
        assertEquals("Acme", result.getName());

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertEquals("Acme", tenantCaptor.getValue().getName());

        ArgumentCaptor<TenantMember> memberCaptor = ArgumentCaptor.forClass(TenantMember.class);
        verify(tenantMemberRepository).save(memberCaptor.capture());
        assertEquals(10L, memberCaptor.getValue().getTenantId());
        assertEquals(1L, memberCaptor.getValue().getUserId());
        assertEquals("owner", memberCaptor.getValue().getRole());
    }

    @Test
    void create_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tenantService.create("Acme", 99L));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("用户"));
    }

    // ---------- getByUserId ----------

    @Test
    void getByUserId_success() {
        TenantMember member = TenantMember.builder().tenantId(10L).userId(1L).build();
        when(tenantMemberRepository.findByUserId(1L)).thenReturn(Optional.of(member));

        Tenant tenant = Tenant.builder().id(10L).name("Acme").build();
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getByUserId(1L);
        assertEquals("Acme", result.getName());
    }

    @Test
    void getByUserId_noMembership_throws() {
        when(tenantMemberRepository.findByUserId(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tenantService.getByUserId(1L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void getByUserId_tenantNotFound_throws() {
        TenantMember member = TenantMember.builder().tenantId(10L).userId(1L).build();
        when(tenantMemberRepository.findByUserId(1L)).thenReturn(Optional.of(member));
        when(tenantRepository.findById(10L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tenantService.getByUserId(1L));
        assertEquals(404, ex.getCode());
    }

    // ---------- getById ----------

    @Test
    void getById_success() {
        Tenant tenant = Tenant.builder().id(10L).name("Acme").build();
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getById(10L);
        assertEquals("Acme", result.getName());
    }

    @Test
    void getById_notFound_throws() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tenantService.getById(99L));
        assertEquals(404, ex.getCode());
    }

    // ---------- update ----------

    @Test
    void update_success() {
        TenantMember operatorMember = TenantMember.builder().tenantId(10L).userId(1L).role("owner").build();
        when(tenantMemberRepository.findByTenantIdAndUserId(10L, 1L)).thenReturn(Optional.of(operatorMember));

        Tenant existing = Tenant.builder().id(10L).name("OldName").build();
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantUpdateRequest req = new TenantUpdateRequest();
        req.setName("NewName");

        Tenant result = tenantService.update(10L, req, 1L);
        assertEquals("NewName", result.getName());
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    void update_updatesWechatAppId() {
        TenantMember operatorMember = TenantMember.builder().tenantId(10L).userId(1L).role("admin").build();
        when(tenantMemberRepository.findByTenantIdAndUserId(10L, 1L)).thenReturn(Optional.of(operatorMember));

        Tenant existing = Tenant.builder().id(10L).name("Acme").build();
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantUpdateRequest req = new TenantUpdateRequest();
        req.setWechatAppId("wx123456");

        Tenant result = tenantService.update(10L, req, 1L);
        assertEquals("wx123456", result.getWechatAppid());
    }

    @Test
    void update_noPermission_throws() {
        when(tenantMemberRepository.findByTenantIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        TenantUpdateRequest req = new TenantUpdateRequest();
        req.setName("NewName");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tenantService.update(10L, req, 1L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void update_memberRoleNotAdmin_throws() {
        TenantMember member = TenantMember.builder().tenantId(10L).userId(1L).role("member").build();
        when(tenantMemberRepository.findByTenantIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));

        TenantUpdateRequest req = new TenantUpdateRequest();
        req.setName("NewName");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tenantService.update(10L, req, 1L));
        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("管理员"));
    }

    // ---------- addMember ----------

    @Test
    void addMember_success() {
        TenantMember operatorMember = TenantMember.builder().tenantId(10L).userId(1L).role("owner").build();
        when(tenantMemberRepository.findByTenantIdAndUserId(10L, 1L)).thenReturn(Optional.of(operatorMember));

        User newUser = User.builder().id(2L).username("bob").build();
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(newUser));
        when(tenantMemberRepository.existsByTenantIdAndUserId(10L, 2L)).thenReturn(false);
        when(tenantMemberRepository.save(any(TenantMember.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantMember result = tenantService.addMember(10L, "bob", "admin", 1L);

        assertEquals(10L, result.getTenantId());
        assertEquals(2L, result.getUserId());
        assertEquals("admin", result.getRole());
    }

    @Test
    void addMember_userNotFound_throws() {
        TenantMember operatorMember = TenantMember.builder().tenantId(10L).userId(1L).role("owner").build();
        when(tenantMemberRepository.findByTenantIdAndUserId(10L, 1L)).thenReturn(Optional.of(operatorMember));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tenantService.addMember(10L, "ghost", "member", 1L));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("用户"));
    }

    @Test
    void addMember_alreadyMember_throws() {
        TenantMember operatorMember = TenantMember.builder().tenantId(10L).userId(1L).role("owner").build();
        when(tenantMemberRepository.findByTenantIdAndUserId(10L, 1L)).thenReturn(Optional.of(operatorMember));

        User existingUser = User.builder().id(2L).username("bob").build();
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(existingUser));
        when(tenantMemberRepository.existsByTenantIdAndUserId(10L, 2L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tenantService.addMember(10L, "bob", "member", 1L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("已经是租户成员"));
    }

    @Test
    void addMember_noPermission_throws() {
        when(tenantMemberRepository.findByTenantIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tenantService.addMember(10L, "bob", "member", 1L));
        assertEquals(403, ex.getCode());
    }

    // ---------- getMembers ----------

    @Test
    void getMembers_success() {
        TenantMember operatorMember = TenantMember.builder().tenantId(10L).userId(1L).role("owner").build();
        when(tenantMemberRepository.findByTenantIdAndUserId(10L, 1L)).thenReturn(Optional.of(operatorMember));

        List<TenantMember> members = List.of(
                TenantMember.builder().tenantId(10L).userId(1L).role("owner").build(),
                TenantMember.builder().tenantId(10L).userId(2L).role("member").build()
        );
        when(tenantMemberRepository.findByTenantId(10L)).thenReturn(members);

        List<TenantMember> result = tenantService.getMembers(10L, 1L);
        assertEquals(2, result.size());
    }

    @Test
    void getMembers_noPermission_throws() {
        when(tenantMemberRepository.findByTenantIdAndUserId(10L, 99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tenantService.getMembers(10L, 99L));
        assertEquals(403, ex.getCode());
    }

    // ---------- isMember ----------

    @Test
    void isMember_true() {
        when(tenantMemberRepository.existsByTenantIdAndUserId(10L, 1L)).thenReturn(true);
        assertTrue(tenantService.isMember(10L, 1L));
    }

    @Test
    void isMember_false() {
        when(tenantMemberRepository.existsByTenantIdAndUserId(10L, 99L)).thenReturn(false);
        assertFalse(tenantService.isMember(10L, 99L));
    }
}
