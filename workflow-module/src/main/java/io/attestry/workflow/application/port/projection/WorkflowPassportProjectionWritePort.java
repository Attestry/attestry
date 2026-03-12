package io.attestry.workflow.application.port.projection;

import java.time.Instant;

public interface WorkflowPassportProjectionWritePort {

    void refreshStateAndCatalog(String passportId, String sourceEventId, Long sourceEventVersion, Instant updatedAt);

    void upsertOwnership(String passportId, String ownerId, String sourceEventId, Long sourceEventVersion, Instant updatedAt);

    void syncPermissionById(String permissionId, String sourceEventId, Long sourceEventVersion, Instant updatedAt);

    void revokeServiceRequestPermissions(String linkedServiceRequestId, String sourceEventId, Instant updatedAt);

    void revokeConsentPermissions(String passportId, String providerTenantId, String sourceEventId, Instant updatedAt);
}
