package io.attestry.workflow.application.port;

import io.attestry.workflow.domain.delegation.model.Delegation;
import java.util.List;
import java.util.Optional;

public interface DelegationRepositoryPort {
    Delegation save(Delegation delegation);

    Optional<Delegation> findById(String delegationId);

    List<Delegation> findByTenantId(String tenantId);

    Optional<Delegation> findActive(
        String brandTenantId,
        String partnerTenantId,
        String resourceType,
        String resourceId,
        String permissionCode
    );

    boolean existsActive(
        String brandTenantId,
        String partnerTenantId,
        String resourceType,
        String resourceId,
        String permissionCode
    );
}
