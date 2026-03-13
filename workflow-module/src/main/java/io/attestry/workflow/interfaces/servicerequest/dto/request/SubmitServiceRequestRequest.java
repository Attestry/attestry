package io.attestry.workflow.interfaces.servicerequest.dto.request;

import java.time.Instant;

public record SubmitServiceRequestRequest(
    String passportId,
    String providerTenantId,
    String beforeEvidenceGroupId,
    String serviceRequestMethod,
    String symptomDescription,
    Instant requestedReservationAt,
    String contactMemo
) {
}
