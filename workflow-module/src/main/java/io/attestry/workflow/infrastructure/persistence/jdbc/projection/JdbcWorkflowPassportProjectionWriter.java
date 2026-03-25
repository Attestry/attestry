package io.attestry.workflow.infrastructure.persistence.jdbc.projection;

import io.attestry.product.application.port.projection.ProductRetailAccessProjectionWritePort;
import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort;
import java.sql.Timestamp;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcWorkflowPassportProjectionWriter implements WorkflowPassportProjectionWritePort {

    private final WorkflowPassportStateCatalogProjectionRepository stateCatalogProjectionRepository;
    private final WorkflowPassportOwnershipProjectionRepository ownershipProjectionRepository;
    private final WorkflowPassportPermissionProjectionRepository permissionProjectionRepository;
    private final ProductRetailAccessProjectionWritePort retailAccessProjectionWriter;

    @Override
    public void refreshStateAndCatalog(ProductStatePayload payload, String sourceEventId, Long sourceEventVersion, Instant updatedAt) {
        stateCatalogProjectionRepository.upsertStateAndCatalog(
            payload,
            sourceEventId,
            sourceEventVersion,
            Timestamp.from(updatedAt)
        );
    }

    @Override
    public void upsertOwnership(String passportId, String ownerId, String sourceEventId, Long sourceEventVersion, Instant updatedAt) {
        ownershipProjectionRepository.upsertOwnership(
            passportId,
            ownerId,
            sourceEventId,
            sourceEventVersion,
            Timestamp.from(updatedAt)
        );
    }

    @Override
    public void syncPermissionById(String permissionId, String sourceEventId, Long sourceEventVersion, Instant updatedAt) {
        permissionProjectionRepository.syncPermissionById(
            permissionId,
            sourceEventId,
            sourceEventVersion,
            Timestamp.from(updatedAt)
        );
        retailAccessProjectionWriter.syncPermissionAccess(permissionId, sourceEventId, updatedAt);
    }

    @Override
    public void revokeServiceRequestPermissions(String linkedServiceRequestId, String sourceEventId, Instant updatedAt) {
        permissionProjectionRepository.revokeByServiceRequest(
            linkedServiceRequestId,
            sourceEventId,
            Timestamp.from(updatedAt)
        );
    }

    @Override
    public void revokeConsentPermissions(String passportId, String providerTenantId, String sourceEventId, Instant updatedAt) {
        permissionProjectionRepository.revokeByConsent(
            passportId,
            providerTenantId,
            sourceEventId,
            Timestamp.from(updatedAt)
        );
    }
}
