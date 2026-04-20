package io.attestry.workflow.interfaces.servicerequest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AcceptServiceRequestRequest(
    @NotBlank(message = "Service type is required")
    String serviceType,
    String description
) {
}
