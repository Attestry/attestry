package io.attestry.workflow.interfaces.delegation;

import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.result.DelegationEvaluateResult;
import io.attestry.workflow.application.delegation.result.DelegationResult;
import io.attestry.workflow.application.usecase.DelegationUseCase;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflows")
public class DelegationHttp {

    private final DelegationUseCase delegationUseCase;

    public DelegationHttp(DelegationUseCase delegationUseCase) {
        this.delegationUseCase = delegationUseCase;
    }

    @PostMapping("/tenants/{brandTenantId}/delegations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_DELEGATION_GRANT')")
    public DelegationResponse grant(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("brandTenantId") String brandTenantId,
        @RequestBody GrantDelegationRequest request
    ) {
        DelegationResult result = delegationUseCase.grant(
            principal,
            brandTenantId,
            new GrantDelegationCommand(
                request.partnerLinkId(),
                request.partnerTenantId(),
                request.resourceType(),
                request.resourceId(),
                request.permissionCode(),
                request.expiresAt(),
                request.note()
            )
        );
        return DelegationResponse.from(result);
    }

    @PostMapping("/delegations/{id}/revoke")
    @PreAuthorize("hasAuthority('SCOPE_DELEGATION_REVOKE')")
    public DelegationResponse revoke(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String delegationId,
        @RequestBody ReasonRequest request
    ) {
        return DelegationResponse.from(delegationUseCase.revoke(principal, delegationId, request.reason()));
    }

    @GetMapping("/tenants/{tenantId}/delegations")
    @PreAuthorize("hasAuthority('SCOPE_DELEGATION_READ')")
    public List<DelegationResponse> list(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId
    ) {
        return delegationUseCase.listByTenant(principal, tenantId).stream().map(DelegationResponse::from).toList();
    }

    @PostMapping("/internal/delegations/evaluate")
    public DelegationEvaluateResponse evaluate(@RequestBody DelegationEvaluateRequest request) {
        DelegationEvaluateResult result = delegationUseCase.evaluate(
            request.brandTenantId(),
            request.partnerTenantId(),
            request.resourceType(),
            request.resourceId(),
            request.permissionCode()
        );
        return new DelegationEvaluateResponse(result.allowed(), result.reason());
    }

    public record GrantDelegationRequest(
        String partnerLinkId,
        String partnerTenantId,
        String resourceType,
        String resourceId,
        String permissionCode,
        Instant expiresAt,
        String note
    ) {
    }

    public record ReasonRequest(String reason) {
    }

    public record DelegationEvaluateRequest(
        String brandTenantId,
        String partnerTenantId,
        String resourceType,
        String resourceId,
        String permissionCode
    ) {
    }

    public record DelegationEvaluateResponse(boolean allowed, String reason) {
    }

    public record DelegationResponse(
        String delegationId,
        String partnerLinkId,
        String brandTenantId,
        String partnerTenantId,
        String resourceType,
        String resourceId,
        String permissionCode,
        String status,
        Instant expiresAt,
        String reason
    ) {
        static DelegationResponse from(DelegationResult result) {
            return new DelegationResponse(
                result.delegationId(),
                result.partnerLinkId(),
                result.brandTenantId(),
                result.partnerTenantId(),
                result.resourceType(),
                result.resourceId(),
                result.permissionCode(),
                result.status(),
                result.expiresAt(),
                result.reason()
            );
        }
    }
}
