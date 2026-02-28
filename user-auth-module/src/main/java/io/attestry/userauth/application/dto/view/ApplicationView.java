package io.attestry.userauth.application.dto.view;

public record ApplicationView(
    String applicationId,
    String type,
    String applicantUserId,
    String tenantId,
    String orgName,
    String country,
    String status,
    String rejectReason
) {
}
