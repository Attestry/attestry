package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.port.servicerequest.ServicePermissionPort;
import io.attestry.workflow.application.servicerequest.command.SubmitServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.SubmitServiceRequestResult;
import io.attestry.workflow.application.servicerequest.support.ServiceRequestContextResolver;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ServiceSubmitExecutor {

    private final ServiceRequestRepository serviceRequestRepository;
    private final WorkflowEvidencePort evidencePort;
    private final ServicePermissionPort servicePermissionPort;
    private final Clock clock;

    public ServiceSubmitExecutor(
        ServiceRequestRepository serviceRequestRepository,
        WorkflowEvidencePort evidencePort,
        ServicePermissionPort servicePermissionPort,
        Clock clock
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.evidencePort = evidencePort;
        this.servicePermissionPort = servicePermissionPort;
        this.clock = clock;
    }

    public SubmitServiceRequestResult submit(
        AuthPrincipal principal,
        SubmitServiceRequestCommand command,
        ServiceRequestContextResolver.SubmitContext context
    ) {
        String permissionId = context.permissionId()
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "No active consent permission found"));

        String beforeEvidenceGroupId = command.beforeEvidenceGroupId();
        if (beforeEvidenceGroupId != null && !beforeEvidenceGroupId.isBlank()) {
            List<String> hashes = evidencePort.findReadyEvidenceHashes(beforeEvidenceGroupId);
            if (hashes.isEmpty()) {
                throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "At least one READY before-evidence is required");
            }
        }

        Instant now = Instant.now(clock);
        String serviceRequestId = UUID.randomUUID().toString();
        ServiceRequest request = ServiceRequest.submit(
            serviceRequestId,
            command.passportId(),
            null,
            context.ownerUserId(),
            command.providerTenantId(),
            null,
            beforeEvidenceGroupId,
            command.serviceRequestMethod(),
            command.symptomDescription(),
            command.requestedReservationAt(),
            command.contactMemo(),
            permissionId,
            principal.userId(),
            now,
            now
        );
        ServiceRequest saved = serviceRequestRepository.save(request);
        servicePermissionPort.linkServiceRequest(permissionId, serviceRequestId);

        return new SubmitServiceRequestResult(
            saved.serviceRequestId(),
            saved.passportId(),
            saved.providerTenantId(),
            null,
            saved.status().name(),
            saved.permissionId(),
            saved.submittedAt()
        );
    }
}
