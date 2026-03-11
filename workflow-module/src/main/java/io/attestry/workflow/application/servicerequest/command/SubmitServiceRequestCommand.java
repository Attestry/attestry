package io.attestry.workflow.application.servicerequest.command;

import java.time.Instant;

public record SubmitServiceRequestCommand(
    String passportId,
    String providerTenantId,
    String beforeEvidenceGroupId,
    String serviceRequestMethod,
    String symptomDescription,
    Instant requestedReservationAt,
    String contactMemo
) {
}
