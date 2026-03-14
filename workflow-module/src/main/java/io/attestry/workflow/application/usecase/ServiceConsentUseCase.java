package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.servicerequest.command.GrantServiceConsentCommand;
import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;

public interface ServiceConsentUseCase {

    GrantServiceConsentResult submit(AuthPrincipal principal, String passportId, GrantServiceConsentCommand command);

    RevokeServiceConsentResult revokeConsent(AuthPrincipal principal, String passportId, String providerTenantId);
}
