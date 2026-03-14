package io.attestry.workflow.interfaces.distribution.dto.response;

public record BatchDistributionEntryResponse(
    String passportId,
    String distributionId,
    String delegationId,
    String status,
    String error
) {
}
