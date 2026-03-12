package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.servicerequest.command.GrantServiceConsentCommand;
import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;
import java.util.List;

public interface ServiceConsentUseCase {

    GrantServiceConsentResult submit(AuthPrincipal principal, String passportId, GrantServiceConsentCommand command);

    RevokeServiceConsentResult revokeConsent(AuthPrincipal principal, String passportId, String providerTenantId);

    PagedServiceProviderResult listServiceProviders(String name, int page, int size);

    ServiceProviderResult getServiceProvider(String tenantId);

    record ServiceProviderResult(
        String tenantId,
        String name,
        String region,
        String address,
        String type
    ) {
    }

    record PagedServiceProviderResult(
        List<ServiceProviderResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }
}
