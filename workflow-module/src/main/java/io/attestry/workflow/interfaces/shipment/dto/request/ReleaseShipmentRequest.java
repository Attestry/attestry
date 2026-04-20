package io.attestry.workflow.interfaces.shipment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReleaseShipmentRequest(
    @NotBlank(message = "Evidence group ID is required")
    String evidenceGroupId
) {
}
