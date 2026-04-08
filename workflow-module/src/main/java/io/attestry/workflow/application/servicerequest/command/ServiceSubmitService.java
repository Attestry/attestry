package io.attestry.workflow.application.servicerequest.command;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.result.SubmitServiceRequestResult;
import io.attestry.workflow.application.servicerequest.internal.ServiceCompleteExecutor;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestContextResolver;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.servicerequest.policy.ServiceSubmitPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceSubmitService implements ServiceSubmitUseCase {

    private final WorkflowAuthorizationSupport authorizationSupport;
    private final ServiceRequestContextResolver contextResolver;
    private final ServiceSubmitPolicy submitPolicy;
    private final ServiceCompleteExecutor.ServiceSubmitExecutor submitExecutor;

    @Override
    @Transactional
    public SubmitServiceRequestResult submit(
        WorkflowActorContext principal,
        SubmitServiceRequestCommand command
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:submit:" + command.passportId());
        ServiceRequestContextResolver.SubmitContext context = contextResolver.resolveSubmitContext(
            principal.userId(),
            command.passportId(),
            command.providerTenantId()
        );
        submitPolicy.assertSubmittable(context.toPolicyContext());
        return submitExecutor.submit(principal, command, context);
    }
}
