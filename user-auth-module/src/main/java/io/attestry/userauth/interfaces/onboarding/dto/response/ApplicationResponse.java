package io.attestry.userauth.interfaces.onboarding.dto.response;

import io.attestry.userauth.application.dto.result.ApplicationResult;
import io.attestry.userauth.application.dto.view.ApplicationView;

public record ApplicationResponse(
    String applicationId,
    String type,
    String applicantUserId,
    String tenantId,
    String orgName,
    String country,
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
            app.status(),
            app.rejectReason()
        );
    }

    public static ApplicationResponse from(ApplicationView app) {
        return new ApplicationResponse(
            app.applicationId(),
            app.type(),
            app.applicantUserId(),
            app.tenantId(),
            app.orgName(),
            app.country(),
            app.status(),
            app.rejectReason()
        );
    }
}
