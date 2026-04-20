package io.attestry.workflow.interfaces.distribution.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RecallRequest(
    @NotBlank(message = "Reason is required")
    String reason
) {
}
