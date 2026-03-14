package io.attestry.workflow.application.servicerequest.command;

import java.time.Instant;

public record GrantServiceConsentCommand(
    String providerTenantId,
    String beforeEvidenceGroupId,
    String serviceRequestMethod,
    String symptomDescription,
    Instant requestedReservationAt,
    String contactMemo
) {
}
