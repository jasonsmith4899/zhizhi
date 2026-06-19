package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.TenantMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TenantMemberRepository extends JpaRepository<TenantMember, Long> {
    List<TenantMember> findByTenantId(Long tenantId);
    Optional<TenantMember> findByUserId(Long userId);
    Optional<TenantMember> findByTenantIdAndUserId(Long tenantId, Long userId);
    boolean existsByTenantIdAndUserId(Long tenantId, Long userId);
}
