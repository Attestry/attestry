package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.servicerequest.ServicePermissionPort;
import io.attestry.workflow.application.port.servicerequest.ServiceProductReadPort;
import io.attestry.workflow.application.servicerequest.command.GrantServiceConsentCommand;
import io.attestry.workflow.application.servicerequest.command.SubmitServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ServiceConsentUseCase;
import io.attestry.workflow.application.usecase.ServiceSubmitUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.policy.ServiceConsentPolicy;
import io.attestry.workflow.domain.servicerequest.policy.ServiceConsentPolicy.ServiceConsentContext;
import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ServiceConsentService implements ServiceConsentUseCase {

    private static final String CONSENT_STATUS_ACTIVE = "ACTIVE";
    private static final String CONSENT_STATUS_REVOKED = "REVOKED";

    private final ServiceProductReadPort serviceProductReadPort;
    private final ServicePermissionPort servicePermissionPort;
    private final ServiceSubmitUseCase serviceSubmitUseCase;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final ServiceConsentPolicy consentPolicy;
    private final Clock clock;


    @Override
    @Transactional
    public GrantServiceConsentResult submit(
        AuthPrincipal principal,
        String passportId,
        GrantServiceConsentCommand command
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:consent:" + passportId);

        ServiceProductReadPort.ServicePassportState state = serviceProductReadPort.findPassportState(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found"));

        String currentOwnerId = serviceProductReadPort.findCurrentOwnerId(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport owner not found"));

        ServiceConsentContext context = new ServiceConsentContext(
            principal.userId(),
            currentOwnerId,
            state.assetState(),
            state.riskFlag()
        );
        consentPolicy.assertConsentGrantable(context);

        Instant now = Instant.now(clock);
        String permissionId = servicePermissionPort.grantServiceRepairConsent(
            passportId,
            command.providerTenantId(),
            principal.userId(),
            now
        );

        var serviceRequest = serviceSubmitUseCase.approve(
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

    @Override
    @Transactional
    public RevokeServiceConsentResult revokeConsent(
        AuthPrincipal principal,
        String passportId,
        String providerTenantId
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:consent-revoke:" + passportId);

        String currentOwnerId = serviceProductReadPort.findCurrentOwnerId(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport owner not found"));

        if (!principal.userId().equals(currentOwnerId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only the passport owner can revoke consent");
        }

        servicePermissionPort.revokeConsentByPassportAndTenant(passportId, providerTenantId);

        return new RevokeServiceConsentResult(passportId, providerTenantId, CONSENT_STATUS_REVOKED);
    }
}
