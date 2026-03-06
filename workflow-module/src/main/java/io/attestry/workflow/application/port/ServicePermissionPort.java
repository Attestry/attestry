package io.attestry.workflow.application.port;

import java.time.Instant;
import java.util.Optional;

public interface ServicePermissionPort {

    String grantServiceRepairPermission(
        String passportId,
        String providerTenantId,
        String linkedServiceRequestId,
        String grantedByUserId,
        Instant now
    );

    void revokeByServiceRequestId(String linkedServiceRequestId);

    boolean hasActiveServiceRepairPermission(String passportId, String providerTenantId);

    String grantServiceRepairConsent(String passportId, String providerTenantId, String grantedByUserId, Instant now);

    void revokeConsentByPassportAndTenant(String passportId, String providerTenantId);

    void linkServiceRequest(String permissionId, String serviceRequestId);

    Optional<String> findActivePermissionId(String passportId, String providerTenantId);
}
