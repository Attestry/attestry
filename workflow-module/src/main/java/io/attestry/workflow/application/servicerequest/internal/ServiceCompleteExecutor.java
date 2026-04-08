package io.attestry.workflow.application.servicerequest.internal;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.port.common.WorkflowLedgerOutboxPort;
import io.attestry.workflow.application.port.servicerequest.ServicePermissionPort;
import io.attestry.workflow.application.servicerequest.command.CompleteServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.command.SubmitServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.CompleteServiceRequestResult;
import io.attestry.workflow.application.event.WorkflowLedgerEvents;
import io.attestry.workflow.application.servicerequest.result.SubmitServiceRequestResult;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceCompleteExecutor {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServicePermissionPort servicePermissionPort;
    private final WorkflowLedgerOutboxPort serviceLedgerOutboxPort;
    private final WorkflowEvidencePort evidencePort;
    private final Clock clock;


    public CompleteServiceRequestResult complete(
        WorkflowActorContext principal,
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
            WorkflowLedgerEvents.serviceConfirmed(saved, beforeHashes, afterHashes, command.serviceResult())
        );

        return new CompleteServiceRequestResult(
            saved.serviceRequestId(),
            saved.passportId(),
            saved.status().name(),
            saved.completedAt(),
            outboxEventId
        );
    }

    @Component
    @RequiredArgsConstructor
    public static class ServiceSubmitExecutor {

        private final ServiceRequestRepository serviceRequestRepository;
        private final WorkflowEvidencePort evidencePort;
        private final ServicePermissionPort servicePermissionPort;
        private final Clock clock;

        public SubmitServiceRequestResult submit(
            WorkflowActorContext principal,
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
}
