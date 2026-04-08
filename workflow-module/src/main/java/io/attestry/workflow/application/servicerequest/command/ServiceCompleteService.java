package io.attestry.workflow.application.servicerequest.command;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.internal.ServiceCompleteExecutor;
import io.attestry.workflow.application.servicerequest.result.CompleteServiceRequestResult;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestContextResolver;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
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
        WorkflowActorContext principal,
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
