package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.ServicePermissionPort;
import io.attestry.workflow.application.port.ServiceProductReadPort;
import io.attestry.workflow.application.servicerequest.command.GrantServiceConsentCommand;
import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ServiceConsentUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.policy.ServiceConsentPolicy;
import io.attestry.workflow.domain.servicerequest.policy.ServiceConsentPolicy.ServiceConsentContext;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceConsentService implements ServiceConsentUseCase {

    private final ServiceProductReadPort serviceProductReadPort;
    private final ServicePermissionPort servicePermissionPort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final ServiceConsentPolicy consentPolicy;
    private final Clock clock;

    public ServiceConsentService(
        ServiceProductReadPort serviceProductReadPort,
        ServicePermissionPort servicePermissionPort,
        WorkflowAuthorizationSupport authorizationSupport,
        ServiceConsentPolicy consentPolicy,
        Clock clock
    ) {
        this.serviceProductReadPort = serviceProductReadPort;
        this.servicePermissionPort = servicePermissionPort;
        this.authorizationSupport = authorizationSupport;
        this.consentPolicy = consentPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public GrantServiceConsentResult grantConsent(
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
            command.providerGroupId(),
            principal.userId(),
            now
        );

        return new GrantServiceConsentResult(
            permissionId,
            passportId,
            command.providerGroupId(),
            "ACTIVE",
            now
        );
    }

    @Override
    @Transactional
    public RevokeServiceConsentResult revokeConsent(
        AuthPrincipal principal,
        String passportId,
        String providerGroupId
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:consent-revoke:" + passportId);

        String currentOwnerId = serviceProductReadPort.findCurrentOwnerId(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport owner not found"));

        if (!principal.userId().equals(currentOwnerId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only the passport owner can revoke consent");
        }

        servicePermissionPort.revokeConsentByPassportAndGroup(passportId, providerGroupId);

        return new RevokeServiceConsentResult(passportId, providerGroupId, "REVOKED");
    }
}
