package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import java.time.Instant;
import java.util.List;

public interface ServiceRequestQueryUseCase {

    PagedServiceRequestResult listMyRequests(AuthPrincipal principal, String status, int page, int size);

    PagedServiceRequestResult listProviderRequests(AuthPrincipal principal, String tenantId, String status, int page, int size);

    record ServiceRequestListItemResult(
        String serviceRequestId,
        String passportId,
        String serialNumber,
        String modelName,
        String providerTenantId,
        String providerTenantName,
        String serviceType,
        String description,
        String status,
        Instant submittedAt
    ) {
    }

    record PagedServiceRequestResult(
        List<ServiceRequestListItemResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }
}
