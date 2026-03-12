package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.port.common.WorkflowLedgerOutboxPort;
import io.attestry.workflow.application.port.servicerequest.ServicePermissionPort;
import io.attestry.workflow.application.servicerequest.command.CompleteServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.CompleteServiceRequestResult;
import io.attestry.workflow.application.servicerequest.support.ServiceRequestContextResolver;
import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ServiceCompleteExecutor {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServicePermissionPort servicePermissionPort;
    private final WorkflowLedgerOutboxPort serviceLedgerOutboxPort;
    private final WorkflowEvidencePort evidencePort;
    private final Clock clock;

    public ServiceCompleteExecutor(
        ServiceRequestRepository serviceRequestRepository,
        ServicePermissionPort servicePermissionPort,
        WorkflowLedgerOutboxPort serviceLedgerOutboxPort,
        WorkflowEvidencePort evidencePort,
        Clock clock
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.servicePermissionPort = servicePermissionPort;
        this.serviceLedgerOutboxPort = serviceLedgerOutboxPort;
        this.evidencePort = evidencePort;
        this.clock = clock;
    }

    public CompleteServiceRequestResult complete(
        AuthPrincipal principal,
        String serviceRequestId,
        CompleteServiceRequestCommand command,
        ServiceRequestContextResolver.CompleteContext context
    ) {
        List<String> afterHashes = List.of();
        String afterEvidenceGroupId = command.afterEvidenceGroupId();
        if (afterEvidenceGroupId != null && !afterEvidenceGroupId.isBlank()) {
            afterHashes = evidencePort.findReadyEvidenceHashes(afterEvidenceGroupId);
            if (afterHashes.isEmpty()) {
                throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "At least one READY after-evidence is required");
            }
        }

        ServiceRequest request = context.request();
        List<String> beforeHashes = List.of();
        if (request.beforeEvidenceGroupId() != null && !request.beforeEvidenceGroupId().isBlank()) {
            beforeHashes = evidencePort.findReadyEvidenceHashes(request.beforeEvidenceGroupId());
        }

        Instant now = Instant.now(clock);
        ServiceRequest completed = request.complete(
            principal.userId(),
            command.serviceType(),
            afterEvidenceGroupId,
            command.serviceResult(),
            command.completionMemo(),
            now
        );
        ServiceRequest saved = serviceRequestRepository.save(completed);
        servicePermissionPort.revokeByServiceRequestId(serviceRequestId);
        String outboxEventId = serviceLedgerOutboxPort.enqueue(
            WorkflowLedgerEventEnvelope.serviceConfirmed(saved, beforeHashes, afterHashes, command.serviceResult())
        );

        return new CompleteServiceRequestResult(
            saved.serviceRequestId(),
            saved.passportId(),
            saved.status().name(),
            saved.completedAt(),
            outboxEventId
        );
    }
}
