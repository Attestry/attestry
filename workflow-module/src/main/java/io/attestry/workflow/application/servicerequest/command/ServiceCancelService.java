package io.attestry.workflow.application.servicerequest.command;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.port.servicerequest.ServicePermissionPort;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestAccessPolicy;
import io.attestry.workflow.application.servicerequest.result.CancelServiceRequestResult;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestResultFactory;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestLookupService;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceCancelService implements ServiceCancelUseCase {

    private final ServiceRequestAccessPolicy accessPolicy;
    private final ServiceRequestLookupService serviceRequestLookupService;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServicePermissionPort servicePermissionPort;
    private final ServiceRequestResultFactory resultFactory;
    private final Clock clock;


    @Override
    @Transactional
    public CancelServiceRequestResult cancel(
        WorkflowActorContext principal,
        String serviceRequestId,
        String cancelReason
    ) {
        accessPolicy.assertOwnerCreatePermission(principal, "service:cancel:" + serviceRequestId);

        ServiceRequest request = serviceRequestLookupService.getPendingById(serviceRequestId);
        accessPolicy.assertOwnerRequestAccess(principal, request, "cancel");

        Instant now = Instant.now(clock);
        ServiceRequest cancelled = request.cancel(cancelReason, now);
        ServiceRequest saved = serviceRequestRepository.save(cancelled);

        servicePermissionPort.revokeByServiceRequestId(serviceRequestId);
        return resultFactory.toCancelResult(saved);
    }
}
