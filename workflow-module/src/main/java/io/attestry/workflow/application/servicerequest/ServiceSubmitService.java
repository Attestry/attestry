package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.ServicePermissionPort;
import io.attestry.workflow.application.port.ServiceProductReadPort;
import io.attestry.workflow.application.port.TenantReadPort;
import io.attestry.workflow.application.port.WorkflowEvidencePort;
import io.attestry.workflow.application.servicerequest.command.SubmitServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.SubmitServiceRequestResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ServiceSubmitUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.policy.ServiceSubmitPolicy;
import io.attestry.workflow.domain.servicerequest.policy.ServiceSubmitPolicy.ServiceSubmitContext;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceSubmitService implements ServiceSubmitUseCase {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceProductReadPort serviceProductReadPort;
    private final ServicePermissionPort servicePermissionPort;
    private final TenantReadPort tenantReadPort;
    private final WorkflowEvidencePort evidencePort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final ServiceSubmitPolicy submitPolicy;
    private final Clock clock;

    public ServiceSubmitService(
        ServiceRequestRepository serviceRequestRepository,
        ServiceProductReadPort serviceProductReadPort,
        ServicePermissionPort servicePermissionPort,
        TenantReadPort tenantReadPort,
        WorkflowEvidencePort evidencePort,
        WorkflowAuthorizationSupport authorizationSupport,
        ServiceSubmitPolicy submitPolicy,
        Clock clock
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.serviceProductReadPort = serviceProductReadPort;
        this.servicePermissionPort = servicePermissionPort;
        this.tenantReadPort = tenantReadPort;
        this.evidencePort = evidencePort;
        this.authorizationSupport = authorizationSupport;
        this.submitPolicy = submitPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubmitServiceRequestResult approve(
        AuthPrincipal principal,
        SubmitServiceRequestCommand command
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:approve:" + command.passportId());

        String passportId = command.passportId();
        String providerTenantId = command.providerTenantId();
        TenantReadPort.TenantSummary provider = tenantReadPort.findTenantSummary(providerTenantId);
        if (provider == null || provider.address() == null || provider.address().isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "providerAddress is required");
        }

        ServiceProductReadPort.ServicePassportState state = serviceProductReadPort.findPassportState(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found"));

        String ownerUserId = serviceProductReadPort.findCurrentOwnerId(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport owner not found"));
        if (!principal.userId().equals(ownerUserId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only current owner can submit service request");
        }

        boolean hasPermission = servicePermissionPort.hasActiveServiceRepairPermission(passportId, providerTenantId);

        ServiceSubmitContext context = new ServiceSubmitContext(
            state.assetState(),
            state.riskFlag(),
            hasPermission,
            serviceRequestRepository.existsOpenByPassportId(passportId)
        );
        submitPolicy.assertSubmittable(context);

        String permissionId = servicePermissionPort.findActivePermissionId(passportId, providerTenantId)
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
            passportId,
            null,
            ownerUserId,
            providerTenantId,
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
