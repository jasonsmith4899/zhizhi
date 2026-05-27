package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.model.dto.TenantUpdateRequest;
import com.zhizhi.ai.model.entity.Tenant;
import com.zhizhi.ai.model.entity.TenantMember;
import com.zhizhi.ai.model.entity.User;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.TenantRepository;
import com.zhizhi.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 租户管理服务
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final UserRepository userRepository;

    /**
     * 创建租户（自动绑定创建者为owner）
     */
    @Transactional
    public Tenant create(String name, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户"));

        Tenant tenant = Tenant.builder()
                .name(name)
                .build();
        tenant = tenantRepository.save(tenant);

        // 创建者成为 owner
        TenantMember member = TenantMember.builder()
                .tenantId(tenant.getId())
                .userId(userId)
                .role("owner")
                .build();
        tenantMemberRepository.save(member);

        return tenant;
    }

    /**
     * 获取用户的租户
     */
    public Tenant getByUserId(Long userId) {
        TenantMember member = tenantMemberRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.notFound("租户"));
        return tenantRepository.findById(member.getTenantId())
                .orElseThrow(() -> BusinessException.notFound("租户"));
    }

    /**
     * 获取租户详情
     */
    public Tenant getById(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("租户"));
    }

    /**
     * 更新租户信息
     */
    @Transactional
    public Tenant update(Long tenantId, TenantUpdateRequest updateData, Long userId) {
        checkPermission(tenantId, userId);
        Tenant tenant = getById(tenantId);

        if (updateData.getName() != null) tenant.setName(updateData.getName());
        if (updateData.getWechatAppId() != null) tenant.setWechatAppid(updateData.getWechatAppId());

        return tenantRepository.save(tenant);
    }

    /**
     * 添加租户成员
     */
    @Transactional
    public TenantMember addMember(Long tenantId, String username, String role, Long operatorUserId) {
        checkPermission(tenantId, operatorUserId);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("用户"));

        if (tenantMemberRepository.existsByTenantIdAndUserId(tenantId, user.getId())) {
            throw BusinessException.badRequest("该用户已经是租户成员");
        }

        TenantMember member = TenantMember.builder()
                .tenantId(tenantId)
                .userId(user.getId())
                .role(role)
                .build();
        return tenantMemberRepository.save(member);
    }

    /**
     * 获取租户成员列表
     */
    public List<TenantMember> getMembers(Long tenantId, Long userId) {
        checkPermission(tenantId, userId);
        return tenantMemberRepository.findByTenantId(tenantId);
    }

    /**
     * 验证用户是否是租户成员
     */
    public boolean isMember(Long tenantId, Long userId) {
        return tenantMemberRepository.existsByTenantIdAndUserId(tenantId, userId);
    }

    /**
     * 检查用户是否有管理权限（owner或admin）
     */
    private void checkPermission(Long tenantId, Long userId) {
        TenantMember member = tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> BusinessException.forbidden("无权操作此租户"));
        if (!"owner".equals(member.getRole()) && !"admin".equals(member.getRole())) {
            throw BusinessException.forbidden("需要管理员权限");
        }
    }
}
