package io.attestry.workflow.interfaces.shipment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PresignShipmentEvidenceUploadRequest(
        @NotBlank(message = "Evidence group ID is required")
        String evidenceGroupId,
        @NotBlank(message = "File name is required")
        String fileName,
        @NotBlank(message = "Content type is required")
        String contentType) {
}
