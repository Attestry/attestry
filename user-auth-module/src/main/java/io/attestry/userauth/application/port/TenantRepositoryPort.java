package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.organization.model.Tenant;

public interface TenantRepositoryPort {
    Tenant save(Tenant tenant);
}
