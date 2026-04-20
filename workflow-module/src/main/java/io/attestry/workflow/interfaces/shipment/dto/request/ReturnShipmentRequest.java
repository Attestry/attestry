package io.attestry.workflow.interfaces.shipment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReturnShipmentRequest(
    @NotBlank(message = "Return evidence group ID is required")
    String returnEvidenceGroupId,
    @NotBlank(message = "Reason is required")
    String reason
) {
}
