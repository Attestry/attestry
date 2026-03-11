package io.attestry.workflow.domain.delegation.repository;

import io.attestry.workflow.domain.delegation.model.Delegation;
import java.util.List;
import java.util.Optional;

public interface DelegationRepository {
    Delegation save(Delegation delegation);

    Optional<Delegation> findById(String delegationId);

    Optional<Delegation> findActive(
        String sourceTenantId,
        String targetTenantId,
        String resourceType,
        String resourceId,
        String permissionCode
    );

    boolean existsActive(
        String sourceTenantId,
        String targetTenantId,
        String resourceType,
        String resourceId,
        String permissionCode
    );

    List<Delegation> findActiveByResourceId(String resourceType, String resourceId);
}
