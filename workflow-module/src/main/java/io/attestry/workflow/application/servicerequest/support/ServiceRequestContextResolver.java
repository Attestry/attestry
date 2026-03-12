package io.attestry.workflow.application.servicerequest.support;

import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.port.servicerequest.ServicePermissionPort;
import io.attestry.workflow.application.port.servicerequest.ServiceProductReadPort;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import io.attestry.workflow.domain.servicerequest.policy.ServiceCompletePolicy;
import io.attestry.workflow.domain.servicerequest.policy.ServiceSubmitPolicy;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestContextResolver {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceProductReadPort serviceProductReadPort;
    private final ServicePermissionPort servicePermissionPort;
    private final TenantReadPort tenantReadPort;

    public ServiceRequestContextResolver(
        ServiceRequestRepository serviceRequestRepository,
        ServiceProductReadPort serviceProductReadPort,
        ServicePermissionPort servicePermissionPort,
        TenantReadPort tenantReadPort
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.serviceProductReadPort = serviceProductReadPort;
        this.servicePermissionPort = servicePermissionPort;
        this.tenantReadPort = tenantReadPort;
    }

    public SubmitContext resolveSubmitContext(String requestingUserId, String passportId, String providerTenantId) {
        TenantReadPort.TenantSummary provider = tenantReadPort.findTenantSummary(providerTenantId);
        if (provider == null || provider.address() == null || provider.address().isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "providerAddress is required");
        }

        ServiceProductReadPort.ServicePassportState state = serviceProductReadPort.findPassportState(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found"));

        String ownerUserId = serviceProductReadPort.findCurrentOwnerId(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport owner not found"));
        if (!requestingUserId.equals(ownerUserId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only current owner can submit service request");
        }

        boolean hasPermission = servicePermissionPort.hasActiveServiceRepairPermission(passportId, providerTenantId);
        Optional<String> permissionId = hasPermission
            ? servicePermissionPort.findActivePermissionId(passportId, providerTenantId)
            : Optional.empty();

        return new SubmitContext(
            provider,
            state,
            ownerUserId,
            hasPermission,
            serviceRequestRepository.existsOpenByPassportId(passportId),
            permissionId
        );
    }

    public CompleteContext resolveCompleteContext(String tenantId, String serviceRequestId) {
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
        return new CompleteContext(request, state, hasPermission);
    }

    public record SubmitContext(
        TenantReadPort.TenantSummary provider,
        ServiceProductReadPort.ServicePassportState state,
        String ownerUserId,
        boolean hasActivePermission,
        boolean openRequestExists,
        Optional<String> permissionId
    ) {
        public ServiceSubmitPolicy.ServiceSubmitContext toPolicyContext() {
            return new ServiceSubmitPolicy.ServiceSubmitContext(
                state.assetState(),
                state.riskFlag(),
                hasActivePermission,
                openRequestExists
            );
        }
    }

    public record CompleteContext(
        ServiceRequest request,
        ServiceProductReadPort.ServicePassportState state,
        boolean hasActivePermission
    ) {
        public ServiceCompletePolicy.ServiceCompleteContext toPolicyContext() {
            return new ServiceCompletePolicy.ServiceCompleteContext(
                state.assetState(),
                state.riskFlag(),
                hasActivePermission
            );
        }
    }
}
