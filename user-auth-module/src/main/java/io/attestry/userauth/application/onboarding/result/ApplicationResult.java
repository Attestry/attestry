package io.attestry.userauth.application.onboarding.result;

public record ApplicationResult(
    String applicationId,
    String type,
    String applicantUserId,
    String tenantId,
    String orgName,
    String country,
    String address,
    String bizRegNo,
    String evidenceBundleId,
    String status,
    String rejectReason
) {
}
