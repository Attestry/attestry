package io.attestry.workflow.interfaces.servicerequest.dto.request;

import jakarta.validation.constraints.Size;

public record CompleteServiceRequestRequest(
    String serviceType,
    String afterEvidenceGroupId,
    @Size(max = 2000, message = "Completion details must be 2000 characters or less.")
    String serviceResult,
    @Size(max = 2000, message = "Additional notes must be 2000 characters or less.")
    String completionMemo
) {
}
