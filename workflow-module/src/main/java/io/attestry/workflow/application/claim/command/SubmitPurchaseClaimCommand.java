package io.attestry.workflow.application.claim.command;

public record SubmitPurchaseClaimCommand(
    String tenantId,
    String groupId,
    String serialNumber,
    String modelName,
    String evidenceGroupId,
    String note
) {
}
