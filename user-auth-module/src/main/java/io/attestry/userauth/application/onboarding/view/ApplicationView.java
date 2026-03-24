package io.attestry.userauth.application.onboarding.view;

import java.util.List;

public record ApplicationView(
    String applicationId,
    String type,
    String applicantUserId,
    String tenantId,
    String orgName,
    String country,
    String address,
    String bizRegNo,
    String evidenceBundleId,
    List<EvidenceFileView> evidenceFiles,
    String status,
    String rejectReason
) {
    public record EvidenceFileView(
        String evidenceFileId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String downloadUrl
    ) {
    }
}
