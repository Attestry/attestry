package io.attestry.workflow.application.servicerequest.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.port.servicerequest.ServiceProductReadPort;
import io.attestry.workflow.application.servicerequest.policy.ServiceRequestAccessPolicy;
import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;
import io.attestry.workflow.application.servicerequest.support.ServiceConsentExecutor;
import io.attestry.workflow.application.servicerequest.usecase.ServiceConsentUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.policy.ServiceConsentPolicy;
import io.attestry.workflow.domain.servicerequest.policy.ServiceConsentPolicy.ServiceConsentContext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ServiceConsentService implements ServiceConsentUseCase {

    private final ServiceProductReadPort serviceProductReadPort;
    private final ServiceRequestAccessPolicy accessPolicy;
    private final ServiceConsentPolicy consentPolicy;
    private final ServiceConsentExecutor consentExecutor;


    @Override
    @Transactional
    public GrantServiceConsentResult submit(
        WorkflowActorContext principal,
        String passportId,
        GrantServiceConsentCommand command
    ) {
        accessPolicy.assertOwnerCreatePermission(principal, "service:consent:" + passportId);

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
        return consentExecutor.grant(principal, passportId, command);
    }

    @Override
    @Transactional
    public RevokeServiceConsentResult revokeConsent(
        WorkflowActorContext principal,
        String passportId,
        String providerTenantId
    ) {
        accessPolicy.assertOwnerCreatePermission(principal, "service:consent-revoke:" + passportId);

        String currentOwnerId = serviceProductReadPort.findCurrentOwnerId(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport owner not found"));
        accessPolicy.assertOwnerConsentAccess(principal, currentOwnerId, "revoke consent");
        return consentExecutor.revoke(passportId, providerTenantId);
    }
}
