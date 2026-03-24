package io.attestry.workflow.application.servicerequest.command;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.port.servicerequest.ServicePermissionPort;
import io.attestry.workflow.application.servicerequest.policy.ServiceRequestAccessPolicy;
import io.attestry.workflow.application.servicerequest.result.CancelServiceRequestResult;
import io.attestry.workflow.application.servicerequest.usecase.ServiceCancelUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceCancelService implements ServiceCancelUseCase {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServicePermissionPort servicePermissionPort;
    private final ServiceRequestAccessPolicy accessPolicy;
    private final Clock clock;


    @Override
    @Transactional
    public CancelServiceRequestResult cancel(
        WorkflowActorContext principal,
        String serviceRequestId,
        String cancelReason
    ) {
        accessPolicy.assertOwnerCreatePermission(principal, "service:cancel:" + serviceRequestId);

        ServiceRequest request = serviceRequestRepository.findById(serviceRequestId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_NOT_FOUND, "Service request not found"));

        if (request.status() != ServiceRequestStatus.PENDING) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE,
                "Only PENDING service request can be cancelled"
            );
        }
        accessPolicy.assertOwnerRequestAccess(principal, request, "cancel");

        Instant now = Instant.now(clock);
        ServiceRequest cancelled = request.cancel(cancelReason, now);
        ServiceRequest saved = serviceRequestRepository.save(cancelled);

        servicePermissionPort.revokeByServiceRequestId(serviceRequestId);

        return new CancelServiceRequestResult(
            saved.serviceRequestId(),
            saved.passportId(),
            saved.status().name(),
            saved.cancelledAt()
        );
    }
}
