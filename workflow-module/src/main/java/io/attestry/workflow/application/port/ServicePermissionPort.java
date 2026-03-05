package io.attestry.workflow.application.port;

import java.time.Instant;
import java.util.Optional;

public interface ServicePermissionPort {

    String grantServiceRepairPermission(
        String passportId,
        String providerGroupId,
        String linkedServiceRequestId,
        String grantedByUserId,
        Instant now
    );

    void revokeByServiceRequestId(String linkedServiceRequestId);

    boolean hasActiveServiceRepairPermission(String passportId, String providerGroupId);

    String grantServiceRepairConsent(String passportId, String providerGroupId, String grantedByUserId, Instant now);

    void revokeConsentByPassportAndGroup(String passportId, String providerGroupId);

    void linkServiceRequest(String permissionId, String serviceRequestId);

    Optional<String> findActivePermissionId(String passportId, String providerGroupId);
}
