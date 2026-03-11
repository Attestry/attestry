package io.attestry.workflow.application.servicerequest;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.servicerequest.ServiceProductReadPort;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.servicerequest.assembler.ServiceRequestQueryViewAssembler;
import io.attestry.workflow.application.servicerequest.support.ServiceRequestStatusParser;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ServiceRequestQueryUseCase;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceRequestQueryService implements ServiceRequestQueryUseCase {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceProductReadPort serviceProductReadPort;
    private final TenantReadPort tenantReadPort;
    private final ServiceRequestStatusParser statusParser;
    private final ServiceRequestQueryViewAssembler viewAssembler;
    private final WorkflowAuthorizationSupport authorizationSupport;

    @Override
    @Transactional(readOnly = true)
    public PagedServiceRequestResult listMyRequests(AuthPrincipal principal, String status, int page, int size) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:list:owner");

        ServiceRequestStatus requestStatus = statusParser.parse(status);
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
        return viewAssembler.toPagedResult(content, safePage, safeSize, totalElements);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedServiceRequestResult listProviderRequests(AuthPrincipal principal, String tenantId, String status, int page, int size) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY, "service:list:provider");

        ServiceRequestStatus requestStatus = statusParser.parse(status);
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
        return viewAssembler.toPagedResult(content, safePage, safeSize, totalElements);
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
                .orElse(viewAssembler.defaultAssetInfo(passportId))
        );
        return viewAssembler.toListItem(request, providerTenantName, passportAssetInfo);
    }
}
