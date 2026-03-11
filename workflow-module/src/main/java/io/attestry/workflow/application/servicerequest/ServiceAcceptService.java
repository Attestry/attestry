package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.servicerequest.command.AcceptServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.AcceptServiceRequestResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ServiceAcceptUseCase;
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
public class ServiceAcceptService implements ServiceAcceptUseCase {

    private final ServiceRequestRepository serviceRequestRepository;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final Clock clock;

    public ServiceAcceptService(
        ServiceRequestRepository serviceRequestRepository,
        WorkflowAuthorizationSupport authorizationSupport,
        Clock clock
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.authorizationSupport = authorizationSupport;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AcceptServiceRequestResult accept(
        AuthPrincipal principal,
        String tenantId,
        String serviceRequestId,
        AcceptServiceRequestCommand command
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.SERVICE_COMPLETE, "service:accept:" + serviceRequestId);

        ServiceRequest request = serviceRequestRepository.findById(serviceRequestId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_NOT_FOUND, "Service request not found"));

        if (!tenantId.equals(request.providerTenantId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant service request access denied");
        }
        if (request.status() != ServiceRequestStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, "Only PENDING service request can be accepted");
        }

        Instant now = Instant.now(clock);
        ServiceRequest accepted = request.accept(command.serviceType(), command.description(), now);
        ServiceRequest saved = serviceRequestRepository.save(accepted);

        return new AcceptServiceRequestResult(
            saved.serviceRequestId(),
            saved.passportId(),
            saved.status().name(),
            now
        );
    }
}
