package io.attestry.workflow.interfaces.claim.dto.request;

public record SubmitClaimRequest(
    String serialNumber, String modelName,
    String evidenceGroupId, String note
) {
}
