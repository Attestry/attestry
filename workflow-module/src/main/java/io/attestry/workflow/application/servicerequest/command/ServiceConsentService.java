package io.attestry.workflow.application.servicerequest.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestAccessPolicy;
import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;
import io.attestry.workflow.application.servicerequest.internal.ServiceConsentContextResolver;
import io.attestry.workflow.application.servicerequest.internal.ServiceConsentExecutor;
import io.attestry.workflow.domain.servicerequest.policy.ServiceConsentPolicy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ServiceConsentService implements ServiceConsentUseCase {

    private final ServiceRequestAccessPolicy accessPolicy;
    private final ServiceConsentPolicy consentPolicy;
    private final ServiceConsentContextResolver consentContextResolver;
    private final ServiceConsentExecutor consentExecutor;


    @Override
    @Transactional
    public GrantServiceConsentResult submit(
        WorkflowActorContext principal,
        String passportId,
        GrantServiceConsentCommand command
    ) {
        accessPolicy.assertOwnerCreatePermission(principal, "service:consent:" + passportId);
        var context = consentContextResolver.resolveGrantContext(principal.userId(), passportId);
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
        String currentOwnerId = consentContextResolver.resolveCurrentOwnerId(passportId);
        accessPolicy.assertOwnerConsentAccess(principal, currentOwnerId, "revoke consent");
        return consentExecutor.revoke(passportId, providerTenantId);
    }
}
