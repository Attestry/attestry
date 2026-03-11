package io.attestry.workflow.interfaces.servicerequest.dto.request;

import java.time.Instant;

public record GrantServiceConsentRequest(
    String providerTenantId,
    String beforeEvidenceGroupId,
    String serviceRequestMethod,
    String symptomDescription,
    Instant requestedReservationAt,
    String contactMemo
) {
}
