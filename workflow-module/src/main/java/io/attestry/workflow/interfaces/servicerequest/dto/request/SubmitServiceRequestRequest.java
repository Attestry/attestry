package io.attestry.workflow.interfaces.servicerequest.dto.request;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record SubmitServiceRequestRequest(
    String passportId,
    String providerTenantId,
    String beforeEvidenceGroupId,
    String serviceRequestMethod,
    @Size(max = 1000, message = "Symptom description must be 1000 characters or less.")
    String symptomDescription,
    Instant requestedReservationAt,
    @Size(max = 300, message = "Contact memo must be 300 characters or less.")
    String contactMemo
) {
}
