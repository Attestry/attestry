package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.common.WorkflowLedgerOutboxPort;
import io.attestry.workflow.application.port.servicerequest.ServicePermissionPort;
import io.attestry.workflow.application.port.servicerequest.ServiceProductReadPort;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.servicerequest.command.CompleteServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.CompleteServiceRequestResult;
import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ServiceCompleteUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import io.attestry.workflow.domain.servicerequest.policy.ServiceCompletePolicy;
import io.attestry.workflow.domain.servicerequest.policy.ServiceCompletePolicy.ServiceCompleteContext;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceCompleteService implements ServiceCompleteUseCase {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceProductReadPort serviceProductReadPort;
    private final ServicePermissionPort servicePermissionPort;
    private final WorkflowLedgerOutboxPort serviceLedgerOutboxPort;
    private final WorkflowEvidencePort evidencePort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final ServiceCompletePolicy completePolicy;
    private final Clock clock;

    @Override
    @Transactional
    public CompleteServiceRequestResult complete(
        AuthPrincipal principal,
        String tenantId,
        String serviceRequestId,
        CompleteServiceRequestCommand command
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.SERVICE_COMPLETE, "service:complete:" + serviceRequestId);

        ServiceRequest request = serviceRequestRepository.findById(serviceRequestId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_NOT_FOUND, "Service request not found"));

        if (request.status() != ServiceRequestStatus.ACCEPTED) {
            throw new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, "Only ACCEPTED service request can be completed");
        }
        if (!tenantId.equals(request.providerTenantId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant service request access denied");
        }

        ServiceProductReadPort.ServicePassportState state = serviceProductReadPort.findPassportState(request.passportId())
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found"));

        boolean hasPermission = servicePermissionPort.hasActiveServiceRepairPermission(request.passportId(), tenantId);

        ServiceCompleteContext context = new ServiceCompleteContext(
            state.assetState(),
            state.riskFlag(),
            hasPermission
        );
        completePolicy.assertCompletable(context);

        String afterEvidenceGroupId = command.afterEvidenceGroupId();
        List<String> afterHashes = List.of();
        if (afterEvidenceGroupId != null && !afterEvidenceGroupId.isBlank()) {
            afterHashes = evidencePort.findReadyEvidenceHashes(afterEvidenceGroupId);
            if (afterHashes.isEmpty()) {
                throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "At least one READY after-evidence is required");
            }
        }

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
