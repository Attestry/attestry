package io.attestry.workflow.interfaces.servicerequest.dto.request;

import jakarta.validation.constraints.Size;

public record RejectServiceRequestRequest(
    @Size(max = 1000, message = "Rejection reason must be 1000 characters or less.")
    String reason
) {
}
