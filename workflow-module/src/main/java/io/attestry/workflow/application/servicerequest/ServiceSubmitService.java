package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.ServicePermissionPort;
import io.attestry.workflow.application.port.ServiceProductReadPort;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
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
    private final ShipmentEvidencePort shipmentEvidencePort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final ServiceSubmitPolicy submitPolicy;
    private final Clock clock;

    public ServiceSubmitService(
        ServiceRequestRepository serviceRequestRepository,
        ServiceProductReadPort serviceProductReadPort,
        ServicePermissionPort servicePermissionPort,
        ShipmentEvidencePort shipmentEvidencePort,
        WorkflowAuthorizationSupport authorizationSupport,
        ServiceSubmitPolicy submitPolicy,
        Clock clock
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.serviceProductReadPort = serviceProductReadPort;
        this.servicePermissionPort = servicePermissionPort;
        this.shipmentEvidencePort = shipmentEvidencePort;
        this.authorizationSupport = authorizationSupport;
        this.submitPolicy = submitPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubmitServiceRequestResult submit(
        AuthPrincipal principal,
        String tenantId,
        String groupId,
        SubmitServiceRequestCommand command
    ) {
        authorizationSupport.assertTenantAndGroupContext(principal, tenantId, groupId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.SERVICE_COMPLETE, "service:submit:" + command.passportId());

        String passportId = command.passportId();

        ServiceProductReadPort.ServicePassportState state = serviceProductReadPort.findPassportState(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found"));

        String ownerUserId = serviceProductReadPort.findCurrentOwnerId(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport owner not found"));

        boolean hasPermission = servicePermissionPort.hasActiveServiceRepairPermission(passportId, groupId);

        ServiceSubmitContext context = new ServiceSubmitContext(
            state.assetState(),
            state.riskFlag(),
            hasPermission,
            serviceRequestRepository.existsSubmittedByPassportId(passportId)
        );
        submitPolicy.assertSubmittable(context);

        String permissionId = servicePermissionPort.findActivePermissionId(passportId, groupId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "No active consent permission found"));

        String beforeEvidenceGroupId = command.beforeEvidenceGroupId();
        if (beforeEvidenceGroupId != null && !beforeEvidenceGroupId.isBlank()) {
            List<String> hashes = shipmentEvidencePort.findReadyEvidenceHashes(beforeEvidenceGroupId);
            if (hashes.isEmpty()) {
                throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "At least one READY before-evidence is required");
            }
        }

        Instant now = Instant.now(clock);
        String serviceRequestId = UUID.randomUUID().toString();

        ServiceRequest request = ServiceRequest.submit(
            serviceRequestId,
            passportId,
            command.serviceType(),
            ownerUserId,
            tenantId,
            groupId,
            command.description(),
            beforeEvidenceGroupId,
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
            saved.serviceType(),
            saved.status().name(),
            saved.permissionId(),
            saved.submittedAt()
        );
    }
}
