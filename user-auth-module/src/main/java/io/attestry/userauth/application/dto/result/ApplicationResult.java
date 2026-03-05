package io.attestry.userauth.application.dto.result;

public record ApplicationResult(
    String applicationId,
    String type,
    String applicantUserId,
    String tenantId,
    String orgName,
    String country,
    String bizRegNo,
    String evidenceBundleId,
    String status,
    String rejectReason
) {
}
