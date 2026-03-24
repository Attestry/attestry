package io.attestry.workflow.application.servicerequest.usecase;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.command.GrantServiceConsentCommand;
import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;

public interface ServiceConsentUseCase {

    GrantServiceConsentResult submit(WorkflowActorContext principal, String passportId, GrantServiceConsentCommand command);

    RevokeServiceConsentResult revokeConsent(WorkflowActorContext principal, String passportId, String providerTenantId);
}
