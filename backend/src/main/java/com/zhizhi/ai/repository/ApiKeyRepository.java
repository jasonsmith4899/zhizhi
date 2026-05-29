package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.ApiKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyValue(String keyValue);

    Page<ApiKey> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ApiKey> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    boolean existsByKeyValue(String keyValue);
}
