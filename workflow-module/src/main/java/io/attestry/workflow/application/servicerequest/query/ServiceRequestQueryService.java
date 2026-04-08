package io.attestry.workflow.application.servicerequest.query;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.port.servicerequest.ServiceProductReadPort;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestQueryViewAssembler;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestStatusParser;
import io.attestry.workflow.application.servicerequest.view.PagedServiceRequestView;
import io.attestry.workflow.application.servicerequest.view.ServiceRequestListItemView;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
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
    public PagedServiceRequestView listMyRequests(WorkflowActorContext principal, String status, int page, int size) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:list:owner");

        ServiceRequestStatus requestStatus = statusParser.parse(status);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        List<ServiceRequest> requests = serviceRequestRepository
            .findByOwnerUserId(principal.userId(), requestStatus, safePage, safeSize)
            .stream().toList();
        Map<String, String> providerNames = loadProviderNames(requests);
        Map<String, ServiceProductReadPort.ServicePassportAssetInfo> passportAssetInfoCache = new HashMap<>();
        List<ServiceRequestListItemView> content = requests.stream()
            .map(request -> toListItem(request, providerNames, passportAssetInfoCache))
            .toList();

        long totalElements = serviceRequestRepository.countByOwnerUserId(principal.userId(), requestStatus);
        return viewAssembler.toPagedView(content, safePage, safeSize, totalElements);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedServiceRequestView listProviderRequests(WorkflowActorContext principal, String tenantId, String status, int page, int size) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY, "service:list:provider");

        ServiceRequestStatus requestStatus = statusParser.parse(status);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        List<ServiceRequest> requests = serviceRequestRepository
            .findByProviderTenantId(tenantId, requestStatus, safePage, safeSize)
            .stream().toList();
        Map<String, String> providerNames = loadProviderNames(requests);
        Map<String, ServiceProductReadPort.ServicePassportAssetInfo> passportAssetInfoCache = new HashMap<>();
        List<ServiceRequestListItemView> content = requests.stream()
            .map(request -> toListItem(request, providerNames, passportAssetInfoCache))
            .toList();

        long totalElements = serviceRequestRepository.countByProviderTenantId(tenantId, requestStatus);
        return viewAssembler.toPagedView(content, safePage, safeSize, totalElements);
    }

    private ServiceRequestListItemView toListItem(
        ServiceRequest request,
        Map<String, String> providerNames,
        Map<String, ServiceProductReadPort.ServicePassportAssetInfo> passportAssetInfoCache
    ) {
        String providerTenantName = providerNames.get(request.providerTenantId());
        ServiceProductReadPort.ServicePassportAssetInfo passportAssetInfo = passportAssetInfoCache.computeIfAbsent(
            request.passportId(),
            passportId -> serviceProductReadPort.findPassportAssetInfo(passportId)
                .orElse(viewAssembler.defaultAssetInfo(passportId))
        );
        return viewAssembler.toListItem(request, providerTenantName, passportAssetInfo);
    }

    private Map<String, String> loadProviderNames(List<ServiceRequest> requests) {
        Map<String, TenantReadPort.TenantSummary> summaries = tenantReadPort.findTenantSummariesByIds(
            requests.stream()
                .map(ServiceRequest::providerTenantId)
                .distinct()
                .toList()
        );
        return summaries.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().name()
            ));
    }
}
