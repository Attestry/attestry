package io.attestry.workflow.interfaces.servicerequest.dto.request;

import jakarta.validation.constraints.Size;

public record CancelServiceRequestRequest(
    @Size(max = 1000, message = "Cancellation reason must be 1000 characters or less.")
    String cancelReason
) {
}
