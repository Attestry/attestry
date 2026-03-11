package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.ServicePermissionPort;
import io.attestry.workflow.application.servicerequest.result.CancelServiceRequestResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ServiceCancelUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceCancelService implements ServiceCancelUseCase {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServicePermissionPort servicePermissionPort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final Clock clock;

    public ServiceCancelService(
        ServiceRequestRepository serviceRequestRepository,
        ServicePermissionPort servicePermissionPort,
        WorkflowAuthorizationSupport authorizationSupport,
        Clock clock
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.servicePermissionPort = servicePermissionPort;
        this.authorizationSupport = authorizationSupport;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CancelServiceRequestResult cancel(
        AuthPrincipal principal,
        String serviceRequestId,
        String cancelReason
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:cancel:" + serviceRequestId);

        ServiceRequest request = serviceRequestRepository.findById(serviceRequestId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_NOT_FOUND, "Service request not found"));

        if (request.status() != ServiceRequestStatus.PENDING) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE,
                "Only PENDING service request can be cancelled"
            );
        }
        if (!principal.userId().equals(request.ownerUserId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only the owner can cancel a service request");
        }

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
