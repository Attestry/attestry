package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.servicerequest.command.CompleteServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.CompleteServiceRequestResult;
import io.attestry.workflow.application.servicerequest.support.ServiceRequestContextResolver;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ServiceCompleteUseCase;
import io.attestry.workflow.domain.servicerequest.policy.ServiceCompletePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceCompleteService implements ServiceCompleteUseCase {

    private final WorkflowAuthorizationSupport authorizationSupport;
    private final ServiceRequestContextResolver contextResolver;
    private final ServiceCompletePolicy completePolicy;
    private final ServiceCompleteExecutor completeExecutor;

    @Override
    @Transactional
    public CompleteServiceRequestResult complete(
        AuthPrincipal principal,
        String tenantId,
        String serviceRequestId,
        CompleteServiceRequestCommand command
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.SERVICE_COMPLETE, "service:complete:" + serviceRequestId);
        ServiceRequestContextResolver.CompleteContext context = contextResolver.resolveCompleteContext(tenantId, serviceRequestId);
        completePolicy.assertCompletable(context.toPolicyContext());
        return completeExecutor.complete(principal, serviceRequestId, command, context);
    }
}
