package io.attestry.workflow.interfaces.manual.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PresignPassportManualEvidenceRequest(
    @NotBlank(message = "Evidence group ID is required")
    String evidenceGroupId,
    @NotBlank(message = "File name is required")
    String fileName,
    @NotBlank(message = "Content type is required")
    String contentType
) {
}
