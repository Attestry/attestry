package io.attestry.workflow.application.servicerequest.support;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.port.servicerequest.ServicePermissionPort;
import io.attestry.workflow.application.servicerequest.command.GrantServiceConsentCommand;
import io.attestry.workflow.application.servicerequest.command.SubmitServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;
import io.attestry.workflow.application.servicerequest.usecase.ServiceSubmitUseCase;
import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceConsentExecutor {

    private static final String CONSENT_STATUS_ACTIVE = "ACTIVE";
    private static final String CONSENT_STATUS_REVOKED = "REVOKED";

    private final ServicePermissionPort servicePermissionPort;
    private final ServiceSubmitUseCase serviceSubmitUseCase;
    private final Clock clock;

    public GrantServiceConsentResult grant(
        WorkflowActorContext principal,
        String passportId,
        GrantServiceConsentCommand command
    ) {
        Instant now = Instant.now(clock);
        String permissionId = servicePermissionPort.grantServiceRepairConsent(
            passportId,
            command.providerTenantId(),
            principal.userId(),
            now
        );

        var serviceRequest = serviceSubmitUseCase.submit(
            principal,
            new SubmitServiceRequestCommand(
                passportId,
                command.providerTenantId(),
                command.beforeEvidenceGroupId(),
                command.serviceRequestMethod(),
                command.symptomDescription(),
                command.requestedReservationAt(),
                command.contactMemo()
            )
        );

        return new GrantServiceConsentResult(
            permissionId,
            serviceRequest.serviceRequestId(),
            passportId,
            command.providerTenantId(),
            CONSENT_STATUS_ACTIVE,
            serviceRequest.status(),
            now
        );
    }

    public RevokeServiceConsentResult revoke(String passportId, String providerTenantId) {
        servicePermissionPort.revokeConsentByPassportAndTenant(passportId, providerTenantId);
        return new RevokeServiceConsentResult(passportId, providerTenantId, CONSENT_STATUS_REVOKED);
    }
}
