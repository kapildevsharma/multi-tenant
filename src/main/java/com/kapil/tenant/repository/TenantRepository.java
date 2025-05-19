
package com.kapil.tenant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kapil.tenant.model.Tenant;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByTenantId(String tenantId);
}
