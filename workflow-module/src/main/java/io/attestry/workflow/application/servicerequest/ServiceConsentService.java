package io.attestry.workflow.application.servicerequest;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.ServicePermissionPort;
import io.attestry.workflow.application.port.ServiceProductReadPort;
import io.attestry.workflow.application.port.TenantReadPort;
import io.attestry.workflow.application.servicerequest.command.GrantServiceConsentCommand;
import io.attestry.workflow.application.servicerequest.command.SubmitServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ServiceConsentUseCase;
import io.attestry.workflow.application.usecase.ServiceSubmitUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.policy.ServiceConsentPolicy;
import io.attestry.workflow.domain.servicerequest.policy.ServiceConsentPolicy.ServiceConsentContext;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceConsentService implements ServiceConsentUseCase {

    private final ServiceProductReadPort serviceProductReadPort;
    private final ServicePermissionPort servicePermissionPort;
    private final TenantReadPort tenantReadPort;
    private final ServiceSubmitUseCase serviceSubmitUseCase;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final ServiceConsentPolicy consentPolicy;
    private final Clock clock;

    public ServiceConsentService(
        ServiceProductReadPort serviceProductReadPort,
        ServicePermissionPort servicePermissionPort,
        TenantReadPort tenantReadPort,
        ServiceSubmitUseCase serviceSubmitUseCase,
        WorkflowAuthorizationSupport authorizationSupport,
        ServiceConsentPolicy consentPolicy,
        Clock clock
    ) {
        this.serviceProductReadPort = serviceProductReadPort;
        this.servicePermissionPort = servicePermissionPort;
        this.tenantReadPort = tenantReadPort;
        this.serviceSubmitUseCase = serviceSubmitUseCase;
        this.authorizationSupport = authorizationSupport;
        this.consentPolicy = consentPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public GrantServiceConsentResult submit(
        AuthPrincipal principal,
        String passportId,
        GrantServiceConsentCommand command
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:consent:" + passportId);

        ServiceProductReadPort.ServicePassportState state = serviceProductReadPort.findPassportState(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found"));

        String currentOwnerId = serviceProductReadPort.findCurrentOwnerId(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport owner not found"));

        ServiceConsentContext context = new ServiceConsentContext(
            principal.userId(),
            currentOwnerId,
            state.assetState(),
            state.riskFlag()
        );
        consentPolicy.assertConsentGrantable(context);

        Instant now = Instant.now(clock);
        String permissionId = servicePermissionPort.grantServiceRepairConsent(
            passportId,
            command.providerTenantId(),
            principal.userId(),
            now
        );

        var serviceRequest = serviceSubmitUseCase.approve(
            principal,
            new SubmitServiceRequestCommand(
                passportId,
                command.providerTenantId(),
                null
            )
        );

        return new GrantServiceConsentResult(
            permissionId,
            serviceRequest.serviceRequestId(),
            passportId,
            command.providerTenantId(),
            "ACTIVE",
            serviceRequest.status(),
            now
        );
    }

    @Override
    @Transactional
    public RevokeServiceConsentResult revokeConsent(
        AuthPrincipal principal,
        String passportId,
        String providerTenantId
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, "service:consent-revoke:" + passportId);

        String currentOwnerId = serviceProductReadPort.findCurrentOwnerId(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport owner not found"));

        if (!principal.userId().equals(currentOwnerId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only the passport owner can revoke consent");
        }

        servicePermissionPort.revokeConsentByPassportAndTenant(passportId, providerTenantId);

        return new RevokeServiceConsentResult(passportId, providerTenantId, "REVOKED");
    }

    @Override
    @Transactional(readOnly = true)
    public PagedServiceProviderResult listServiceProviders(String name, int page, int size) {
        TenantReadPort.PagedTenantSummary paged = tenantReadPort.searchActiveTenantsByTypeAndName("SERVICE", name,
            page, size);
        List<ServiceProviderResult> content = paged.content().stream()
            .map(tenant -> new ServiceProviderResult(tenant.tenantId(), tenant.name(), tenant.region(), tenant.type()))
            .toList();
        return new PagedServiceProviderResult(
            content,
            paged.page(),
            paged.size(),
            paged.totalElements(),
            paged.totalPages()
        );
    }
}
