package io.attestry.workflow.interfaces.claim.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SubmitClaimRequest(
    @NotBlank(message = "Serial number is required")
    String serialNumber,
    @NotBlank(message = "Model name is required")
    String modelName,
    @NotBlank(message = "Evidence group ID is required")
    String evidenceGroupId,
    String note
) {
}
