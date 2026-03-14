package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.servicerequest.command.RejectServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.policy.ServiceRequestAccessPolicy;
import io.attestry.workflow.application.servicerequest.result.RejectServiceRequestResult;
import io.attestry.workflow.application.usecase.ServiceRejectUseCase;
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
public class ServiceRejectService implements ServiceRejectUseCase {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestAccessPolicy accessPolicy;
    private final Clock clock;


    @Override
    @Transactional
    public RejectServiceRequestResult reject(
        AuthPrincipal principal,
        String tenantId,
        String serviceRequestId,
        RejectServiceRequestCommand command
    ) {
        accessPolicy.assertProviderCompletePermission(principal, tenantId, "service:reject:" + serviceRequestId);

        ServiceRequest request = serviceRequestRepository.findById(serviceRequestId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_NOT_FOUND, "Service request not found"));

        accessPolicy.assertProviderRequestAccess(tenantId, request, "reject");
        if (request.status() != ServiceRequestStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, "Only PENDING service request can be rejected");
        }

        Instant now = Instant.now(clock);
        ServiceRequest rejected = request.reject(command.reason(), now);
        ServiceRequest saved = serviceRequestRepository.save(rejected);

        return new RejectServiceRequestResult(
            saved.serviceRequestId(),
            saved.passportId(),
            saved.status().name(),
            now
        );
    }
}
