package io.attestry.userauth.application.dto.view;

public record ApplicationView(
    String applicationId,
    String type,
    String applicantUserId,
    String tenantId,
    String orgName,
    String country,
    String bizRegNo,
    String evidenceBundleId,
    String evidenceOriginalFileName,
    String evidenceDownloadUrl,
    String status,
    String rejectReason
) {
}
