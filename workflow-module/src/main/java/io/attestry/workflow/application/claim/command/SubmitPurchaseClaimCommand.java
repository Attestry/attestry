package io.attestry.workflow.application.claim.command;

public record SubmitPurchaseClaimCommand(
    String serialNumber,
    String modelName,
    String evidenceGroupId,
    String note
) {
}
