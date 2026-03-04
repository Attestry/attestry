package io.attestry.userauth.domain.organization.repository;

import io.attestry.userauth.domain.organization.model.Tenant;
import java.util.Optional;

public interface TenantRepository {
    Tenant save(Tenant tenant);

    Optional<Tenant> findById(String tenantId);
}
