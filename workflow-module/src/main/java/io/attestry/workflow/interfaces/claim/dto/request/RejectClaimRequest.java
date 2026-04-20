package io.attestry.workflow.interfaces.claim.dto.request;

import jakarta.validation.constraints.Size;

public record RejectClaimRequest(
    @Size(max = 1000, message = "Rejection reason must be 1000 characters or less.")
    String reason
) {
}
