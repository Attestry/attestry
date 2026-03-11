package io.attestry.userauth.application.port.tenant;

import io.attestry.userauth.domain.tenant.model.Tenant;
import io.attestry.userauth.domain.tenant.model.TenantStatus;
import io.attestry.userauth.domain.tenant.model.TenantType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenantRepositoryPort {
    Tenant save(Tenant tenant);

    Optional<Tenant> findById(String tenantId);

    Page<Tenant> findPage(TenantType type, TenantStatus status, String name, Pageable pageable);
}
