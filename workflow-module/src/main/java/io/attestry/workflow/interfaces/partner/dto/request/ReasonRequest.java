package io.attestry.workflow.interfaces.partner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReasonRequest(
    @NotBlank(message = "Reason is required.")
    @Size(max = 1000, message = "Reason must be 1000 characters or less.")
    String reason
) {
}
