package io.attestry.userauth.interfaces.onboarding.dto.response;

import io.attestry.userauth.application.onboarding.result.ApplicationResult;
import io.attestry.userauth.application.onboarding.view.ApplicationView;
import java.util.List;

public record ApplicationResponse(
    String applicationId,
    String type,
    String applicantUserId,
    String tenantId,
    String orgName,
    String country,
    String address,
    String bizRegNo,
    String evidenceBundleId,
    List<EvidenceFileResponse> evidenceFiles,
    String status,
    String rejectReason
) {
    public static ApplicationResponse from(ApplicationResult app) {
        return new ApplicationResponse(
            app.applicationId(),
            app.type(),
            app.applicantUserId(),
            app.tenantId(),
            app.orgName(),
            app.country(),
            app.address(),
            app.bizRegNo(),
            app.evidenceBundleId(),
            List.of(),
            app.status(),
            app.rejectReason()
        );
    }

    public static ApplicationResponse from(ApplicationView app) {
        List<EvidenceFileResponse> files = app.evidenceFiles().stream()
            .map(f -> new EvidenceFileResponse(
                f.evidenceFileId(), f.originalFileName(), f.contentType(), f.sizeBytes(), f.downloadUrl()
            ))
            .toList();
        return new ApplicationResponse(
            app.applicationId(),
            app.type(),
            app.applicantUserId(),
            app.tenantId(),
            app.orgName(),
            app.country(),
            app.address(),
            app.bizRegNo(),
            app.evidenceBundleId(),
            files,
            app.status(),
            app.rejectReason()
        );
    }

    public record EvidenceFileResponse(
        String evidenceFileId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String downloadUrl
    ) {
    }
}
