package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.ServiceProductReadPort;
import io.attestry.workflow.application.port.TenantReadPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ServiceRequestQueryUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceRequestQueryService implements ServiceRequestQueryUseCase {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceProductReadPort serviceProductReadPort;
    private final TenantReadPort tenantReadPort;
    private final WorkflowAuthorizationSupport authorizationSupport;

    public ServiceRequestQueryService(
        ServiceRequestRepository serviceRequestRepository,
        ServiceProductReadPort serviceProductReadPort,
        TenantReadPort tenantReadPort,
        WorkflowAuthorizationSupport authorizationSupport
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.serviceProductReadPort = serviceProductReadPort;
        this.tenantReadPort = tenantReadPort;
        this.authorizationSupport = authorizationSupport;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedServiceRequestResult listMyRequests(AuthPrincipal principal, String status, int page, int size) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:list:owner");

        ServiceRequestStatus requestStatus = parseStatus(status);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Map<String, String> providerNameCache = new HashMap<>();
        Map<String, ServiceProductReadPort.ServicePassportAssetInfo> passportAssetInfoCache = new HashMap<>();

        List<ServiceRequestListItemResult> content = serviceRequestRepository
            .findByOwnerUserId(principal.userId(), requestStatus, safePage, safeSize)
            .stream()
            .map(request -> toListItem(request, providerNameCache, passportAssetInfoCache))
            .toList();

        long totalElements = serviceRequestRepository.countByOwnerUserId(principal.userId(), requestStatus);
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        return new PagedServiceRequestResult(content, safePage, safeSize, totalElements, totalPages);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedServiceRequestResult listProviderRequests(AuthPrincipal principal, String tenantId, String status, int page, int size) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.SERVICE_COMPLETE, "service:list:provider");

        ServiceRequestStatus requestStatus = parseStatus(status);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Map<String, String> providerNameCache = new HashMap<>();
        Map<String, ServiceProductReadPort.ServicePassportAssetInfo> passportAssetInfoCache = new HashMap<>();

        List<ServiceRequestListItemResult> content = serviceRequestRepository
            .findByProviderTenantId(tenantId, requestStatus, safePage, safeSize)
            .stream()
            .map(request -> toListItem(request, providerNameCache, passportAssetInfoCache))
            .toList();

        long totalElements = serviceRequestRepository.countByProviderTenantId(tenantId, requestStatus);
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        return new PagedServiceRequestResult(content, safePage, safeSize, totalElements, totalPages);
    }

    private ServiceRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ServiceRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Invalid service request status filter");
        }
    }

    private ServiceRequestListItemResult toListItem(
        ServiceRequest request,
        Map<String, String> providerNameCache,
        Map<String, ServiceProductReadPort.ServicePassportAssetInfo> passportAssetInfoCache
    ) {
        String providerTenantName = providerNameCache.computeIfAbsent(
            request.providerTenantId(),
            tenantReadPort::findTenantName
        );
        ServiceProductReadPort.ServicePassportAssetInfo passportAssetInfo = passportAssetInfoCache.computeIfAbsent(
            request.passportId(),
            passportId -> serviceProductReadPort.findPassportAssetInfo(passportId)
                .orElse(new ServiceProductReadPort.ServicePassportAssetInfo(passportId, null, null))
        );
        return new ServiceRequestListItemResult(
            request.serviceRequestId(),
            request.passportId(),
            passportAssetInfo.serialNumber(),
            passportAssetInfo.modelName(),
            request.providerTenantId(),
            providerTenantName,
            request.serviceType(),
            request.description(),
            request.status().name(),
            request.submittedAt()
        );
    }
}
